package com.devteria.reading.mapper;

import com.devteria.reading.dto.response.ChapterResponse;
import com.devteria.reading.dto.response.ChapterSummaryResponse;
import com.devteria.reading.entity.Chapter;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class ChapterMapperImpl implements ChapterMapper {

    @Override
    public ChapterResponse toChapterResponse(Chapter chapter) {
        if ( chapter == null ) {
            return null;
        }

        ChapterResponse.ChapterResponseBuilder chapterResponse = ChapterResponse.builder();

        chapterResponse.id( chapter.getId() );
        chapterResponse.bookId( chapter.getBookId() );
        chapterResponse.chapterNumber( chapter.getChapterNumber() );
        chapterResponse.title( chapter.getTitle() );
        chapterResponse.subtitle( chapter.getSubtitle() );
        chapterResponse.content( chapter.getContent() );
        chapterResponse.readTimeMinutes( chapter.getReadTimeMinutes() );

        return chapterResponse.build();
    }

    @Override
    public ChapterSummaryResponse toChapterSummaryResponse(Chapter chapter) {
        if ( chapter == null ) {
            return null;
        }

        ChapterSummaryResponse.ChapterSummaryResponseBuilder chapterSummaryResponse = ChapterSummaryResponse.builder();

        chapterSummaryResponse.id( chapter.getId() );
        chapterSummaryResponse.bookId( chapter.getBookId() );
        chapterSummaryResponse.chapterNumber( chapter.getChapterNumber() );
        chapterSummaryResponse.title( chapter.getTitle() );
        chapterSummaryResponse.subtitle( chapter.getSubtitle() );
        chapterSummaryResponse.readTimeMinutes( chapter.getReadTimeMinutes() );

        return chapterSummaryResponse.build();
    }
}
