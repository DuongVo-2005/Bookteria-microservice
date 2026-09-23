package com.devteria.reading.mapper;

import com.devteria.reading.dto.response.HighlightResponse;
import com.devteria.reading.entity.Highlight;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class HighlightMapperImpl implements HighlightMapper {

    @Override
    public HighlightResponse toHighlightResponse(Highlight highlight) {
        if ( highlight == null ) {
            return null;
        }

        HighlightResponse.HighlightResponseBuilder highlightResponse = HighlightResponse.builder();

        highlightResponse.id( highlight.getId() );
        highlightResponse.bookId( highlight.getBookId() );
        highlightResponse.chapterId( highlight.getChapterId() );
        highlightResponse.chapterNumber( highlight.getChapterNumber() );
        highlightResponse.chapterTitle( highlight.getChapterTitle() );
        highlightResponse.selectedText( highlight.getSelectedText() );
        highlightResponse.color( highlight.getColor() );
        highlightResponse.note( highlight.getNote() );
        highlightResponse.createdAt( highlight.getCreatedAt() );

        return highlightResponse.build();
    }
}
