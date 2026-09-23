package com.devteria.reading.mapper;

import com.devteria.reading.dto.response.BookmarkResponse;
import com.devteria.reading.entity.Bookmark;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class BookmarkMapperImpl implements BookmarkMapper {

    @Override
    public BookmarkResponse toBookmarkResponse(Bookmark bookmark) {
        if ( bookmark == null ) {
            return null;
        }

        BookmarkResponse.BookmarkResponseBuilder bookmarkResponse = BookmarkResponse.builder();

        bookmarkResponse.id( bookmark.getId() );
        bookmarkResponse.bookId( bookmark.getBookId() );
        bookmarkResponse.chapterId( bookmark.getChapterId() );
        bookmarkResponse.chapterNumber( bookmark.getChapterNumber() );
        bookmarkResponse.chapterTitle( bookmark.getChapterTitle() );
        bookmarkResponse.positionPercent( bookmark.getPositionPercent() );
        bookmarkResponse.snippet( bookmark.getSnippet() );
        bookmarkResponse.note( bookmark.getNote() );
        bookmarkResponse.createdAt( bookmark.getCreatedAt() );

        return bookmarkResponse.build();
    }
}
