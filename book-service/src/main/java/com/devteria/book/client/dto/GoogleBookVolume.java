package com.devteria.book.client.dto;

import lombok.Data;

// accessInfo PHẢI dùng đúng shape thật của Google (GoogleBookAccessInfo, viewability là String,
// epub/pdf là object lồng bên trong) — trước đây trỏ nhầm sang entity.GoogleBooksAccessInfo (shape
// đã "dẹt hoá" dùng để LƯU vào Mongo), khiến Jackson không tìm thấy field epub.isAvailable/
// pdf.isAvailable ở top-level JSON, luôn map ra null dù Google trả đúng dữ liệu. 2 shape này khác
// mục đích: GoogleBookAccessInfo (đây) = deserialize JSON thật của Google; entity.GoogleBooksAccessInfo
// = dữ liệu đã rút gọn để lưu Book, chuyển đổi ở GoogleBooksImportService.
@Data
public class GoogleBookVolume {

    private String id;

    private String selfLink;

    private GoogleBookVolumeInfo volumeInfo;

    private GoogleBookAccessInfo accessInfo;

    private GoogleBookSaleInfo saleInfo;
}
