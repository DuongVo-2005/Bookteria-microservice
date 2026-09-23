package com.devteria.book.client.googlebooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.devteria.book.client.dto.GoogleBookVolume;
import com.devteria.book.client.dto.GoogleBooksVolumeResponse;

// Unit test thuần (không @SpringBootTest, không gọi mạng thật) - dùng MockRestServiceServer bind
// vào đúng RestClient.Builder mà GoogleBooksClient nhận, theo đúng gợi ý ở spec mục 11. Trước đây
// file này là @SpringBootTest gọi thật ra googleapis.com và cần full ApplicationContext (Mongo)
// chỉ để test 1 HTTP client - không lên nổi khi thiếu Mongo, không phải lỗi mạng.
class GoogleBooksClientTest {

    private static final String BASE_URL = "https://www.googleapis.com/books/v1";

    private MockRestServiceServer mockServer;
    private GoogleBooksClient googleBooksClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        googleBooksClient = new GoogleBooksClient(builder.build(), "test-api-key");
    }

    @Test
    void getVolumeById_mapsAvailabilityFlagsCorrectly() {
        String volumeId = "zyTCAlFPjgYC";
        String json = """
				{
				"id": "zyTCAlFPjgYC",
				"volumeInfo": {
					"title": "Java Programming",
					"authors": ["John Doe"],
					"categories": ["Computers"]
				},
				"accessInfo": {
					"viewability": "PARTIAL",
					"embeddable": true,
					"publicDomain": false,
					"webReaderLink": "https://books.google.com/books/reader",
					"epub": { "isAvailable": true },
					"pdf": { "isAvailable": false }
				},
				"saleInfo": {
					"saleability": "FOR_SALE",
					"isEbook": true
				}
				}
				""";

        mockServer
                .expect(requestTo(startsWith(BASE_URL + "/volumes/" + volumeId)))
                .andExpect(queryParam("key", "test-api-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GoogleBookVolume volume = googleBooksClient.getVolumeById(volumeId);

        assertThat(volume.getId()).isEqualTo(volumeId);
        assertThat(volume.getVolumeInfo().getTitle()).isEqualTo("Java Programming");
        assertThat(volume.getVolumeInfo().getAuthors()).containsExactly("John Doe");
        assertThat(volume.getAccessInfo().getEmbeddable()).isTrue();
        assertThat(volume.getAccessInfo().getEpub().getIsAvailable()).isTrue();
        assertThat(volume.getAccessInfo().getPdf().getIsAvailable()).isFalse();
        assertThat(volume.getSaleInfo().getIsEbook()).isTrue();

        mockServer.verify();
    }

    @Test
    void searchVolumes_mapsListResponse() {
        String json = """
				{
				"kind": "books#volumes",
				"totalItems": 1,
				"items": [
					{ "id": "abc123", "volumeInfo": { "title": "Clean Code" } }
				]
				}
				""";

        mockServer
                .expect(requestTo(startsWith(BASE_URL + "/volumes")))
                .andExpect(queryParam("q", "java"))
                .andExpect(queryParam("startIndex", "0"))
                .andExpect(queryParam("maxResults", "10"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GoogleBooksVolumeResponse response = googleBooksClient.searchVolumes("java", 0, 10);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isEqualTo("abc123");
        assertThat(response.getItems().get(0).getVolumeInfo().getTitle()).isEqualTo("Clean Code");

        mockServer.verify();
    }

    @Test
    void getVolumeById_blankApiKey_omitsKeyQueryParam() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleBooksClient clientWithoutKey = new GoogleBooksClient(builder.build(), "");

        String json = """
				{ "id": "abc123", "volumeInfo": { "title": "No Key Needed" } }
				""";

        server.expect(requestTo(BASE_URL + "/volumes/abc123"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GoogleBookVolume volume = clientWithoutKey.getVolumeById("abc123");

        assertThat(volume.getId()).isEqualTo("abc123");
        server.verify();
    }
}
