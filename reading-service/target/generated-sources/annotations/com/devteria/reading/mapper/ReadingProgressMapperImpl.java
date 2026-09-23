package com.devteria.reading.mapper;

import com.devteria.reading.dto.response.ReadingProgressResponse;
import com.devteria.reading.entity.ReadingProgress;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class ReadingProgressMapperImpl implements ReadingProgressMapper {

    @Override
    public ReadingProgressResponse toReadingProgressResponse(ReadingProgress readingProgress) {
        if ( readingProgress == null ) {
            return null;
        }

        ReadingProgressResponse.ReadingProgressResponseBuilder readingProgressResponse = ReadingProgressResponse.builder();

        readingProgressResponse.bookId( readingProgress.getBookId() );
        readingProgressResponse.currentChapterId( readingProgress.getCurrentChapterId() );
        readingProgressResponse.currentChapterIndex( readingProgress.getCurrentChapterIndex() );
        readingProgressResponse.progressPercent( readingProgress.getProgressPercent() );
        readingProgressResponse.status( readingProgress.getStatus() );
        readingProgressResponse.lastReadAt( readingProgress.getLastReadAt() );

        return readingProgressResponse.build();
    }
}
