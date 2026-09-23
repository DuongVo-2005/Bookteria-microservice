package com.devteria.reading.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.devteria.reading.dto.response.ChapterImportResponse;
import com.devteria.reading.entity.Chapter;
import com.devteria.reading.exception.AppException;
import com.devteria.reading.exception.ErrorCode;
import com.devteria.reading.repository.ChapterRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;

// Parser ePub/PDF tự động — lối tạm để nạp dữ liệu chương thật thay cho seed tay từng chương
// qua ChapterController#createChapter (endpoint đó vẫn giữ, dùng cho sửa/thêm lẻ). ePub có cấu
// trúc thật (spine + table of contents) nên tách chương chính xác; PDF không có khái niệm
// "chương" nội tại — chỉ tách được nếu file có outline/bookmark PDF thật, nếu không thì fallback
// an toàn: toàn bộ nội dung thành 1 chương duy nhất, không đoán bừa.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookContentImportService {
    ChapterRepository chapterRepository;

    private static final int WORDS_PER_MINUTE = 200;

    public ChapterImportResponse importFile(String bookId, MultipartFile file) {
        String filename = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);

        List<ParsedChapter> parsedChapters;
        List<String> warnings = new ArrayList<>();
        try {
            if (filename.endsWith(".epub")) {
                parsedChapters = parseEpub(file);
            } else if (filename.endsWith(".pdf")) {
                parsedChapters = parsePdf(file, warnings);
            } else {
                throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse uploaded book content file. bookId={}, filename={}", bookId, filename, e);
            throw new AppException(ErrorCode.FILE_PARSE_FAILED);
        }

        int startNumber = (int) chapterRepository.countByBookId(bookId) + 1;

        int savedCount = 0;
        for (int i = 0; i < parsedChapters.size(); i++) {
            ParsedChapter parsed = parsedChapters.get(i);
            if (parsed.content == null || parsed.content.isBlank()) {
                continue; // trang bìa/mục lục thường không có text -> tự bỏ qua, không tạo chương rỗng
            }
            Chapter chapter = Chapter.builder()
                    .bookId(bookId)
                    .chapterNumber(startNumber + savedCount)
                    .title(parsed.title)
                    .content(parsed.content)
                    .readTimeMinutes(estimateReadTimeMinutes(parsed.content))
                    .build();
            chapterRepository.save(chapter);
            savedCount++;
        }

        return ChapterImportResponse.builder()
                .chaptersCreated(savedCount)
                .warnings(warnings)
                .build();
    }

    // ===== ePub =====

    private List<ParsedChapter> parseEpub(MultipartFile file) throws Exception {
        Book book = new EpubReader().readEpub(file.getInputStream());
        List<ParsedChapter> chapters = new ArrayList<>();

        int index = 1;
        for (var spineRef : book.getSpine().getSpineReferences()) {
            Resource resource = spineRef.getResource();
            String html = new String(resource.getData(), StandardCharsets.UTF_8);
            String content = convertHtmlToContent(html);
            String title = resolveChapterTitle(book, resource, html, index);
            chapters.add(new ParsedChapter(title, content));
            index++;
        }
        return chapters;
    }

    private String resolveChapterTitle(Book book, Resource resource, String html, int index) {
        TOCReference match = findTocReferenceByHref(book.getTableOfContents().getTocReferences(), resource.getHref());
        if (match != null && match.getTitle() != null && !match.getTitle().isBlank()) {
            return match.getTitle().trim();
        }
        var heading = Jsoup.parse(html).selectFirst("h1, h2, h3");
        if (heading != null && !heading.text().isBlank()) {
            return heading.text().trim();
        }
        return "Chương " + index;
    }

    private TOCReference findTocReferenceByHref(List<TOCReference> refs, String href) {
        if (refs == null) return null;
        for (TOCReference ref : refs) {
            if (ref.getResource() != null && href.equals(ref.getResource().getHref())) {
                return ref;
            }
            TOCReference childMatch = findTocReferenceByHref(ref.getChildren(), href);
            if (childMatch != null) return childMatch;
        }
        return null;
    }

    // Chuyển XHTML của 1 chương ePub sang format content hiện có của Chapter (đúng quy ước cũ:
    // đoạn cách nhau 2 dòng trống, "### " = tiêu đề phụ, "> " = trích dẫn, ``` = code block).
    private String convertHtmlToContent(String html) {
        var doc = Jsoup.parse(html);
        Elements blocks =
                doc.body() == null ? new Elements() : doc.body().select("h1, h2, h3, h4, h5, h6, p, blockquote, pre");

        StringBuilder sb = new StringBuilder();
        for (Element el : blocks) {
            String text = el.text().trim();
            if (text.isEmpty()) continue;

            String tag = el.tagName();
            if (tag.matches("h[1-6]")) {
                sb.append("### ").append(text).append("\n\n");
            } else if (tag.equals("blockquote")) {
                sb.append("> ").append(text).append("\n\n");
            } else if (tag.equals("pre")) {
                sb.append("```\n").append(text).append("\n```\n\n");
            } else {
                sb.append(text).append("\n\n");
            }
        }

        if (sb.isEmpty() && doc.body() != null) {
            // Không có block tag nào khớp (XHTML lạ) - fallback lấy toàn bộ text thô, còn hơn rỗng.
            sb.append(doc.body().text());
        }
        return sb.toString().trim();
    }

    // ===== PDF =====

    private List<ParsedChapter> parsePdf(MultipartFile file, List<String> warnings) throws Exception {
        List<ParsedChapter> chapters = new ArrayList<>();
        byte[] bytes = file.getBytes();

        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            List<PDOutlineItem> items = new ArrayList<>();
            if (outline != null) {
                for (PDOutlineItem item : outline.children()) {
                    items.add(item);
                }
            }

            if (items.size() < 2) {
                warnings.add(
                        "Không tìm thấy mục lục (outline/bookmark) trong PDF, toàn bộ nội dung được lưu thành 1 chương — cần tự tách tay nếu muốn chia nhỏ.");
                PDFTextStripper stripper = new PDFTextStripper();
                String content = stripper.getText(document);
                chapters.add(new ParsedChapter("Toàn bộ nội dung", content));
                return chapters;
            }

            int totalPages = document.getNumberOfPages();
            int[] startPages = new int[items.size()];
            for (int i = 0; i < items.size(); i++) {
                PDPage page = items.get(i).findDestinationPage(document);
                startPages[i] = page == null
                        ? 0
                        : document.getDocumentCatalog().getPages().indexOf(page);
            }

            for (int i = 0; i < items.size(); i++) {
                int startIdx = startPages[i];
                int endIdx = (i + 1 < items.size()) ? startPages[i + 1] - 1 : totalPages - 1;
                if (endIdx < startIdx) endIdx = startIdx;

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(startIdx + 1);
                stripper.setEndPage(endIdx + 1);
                String content = stripper.getText(document);

                String title = items.get(i).getTitle();
                chapters.add(new ParsedChapter(
                        title == null || title.isBlank() ? "Chương " + (i + 1) : title.trim(), content));
            }
        }
        return chapters;
    }

    private int estimateReadTimeMinutes(String content) {
        int wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        int minutes = (int) Math.ceil(wordCount / (double) WORDS_PER_MINUTE);
        return Math.max(minutes, 1);
    }

    private record ParsedChapter(String title, String content) {}
}
