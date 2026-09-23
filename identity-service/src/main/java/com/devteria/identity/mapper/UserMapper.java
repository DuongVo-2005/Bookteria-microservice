package com.devteria.identity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.devteria.identity.dto.request.UserCreationRequest;
import com.devteria.identity.dto.request.UserUpdateRequest;
import com.devteria.identity.dto.response.UserResponse;
import com.devteria.identity.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    // password/roles đều được UserService.updateUser() tự xử lý có điều kiện (chỉ đổi khi client
    // thực sự gửi) - MapStruct mặc định vẫn gọi setPassword(null)/setRoles(null) vô điều kiện khi
    // field null nếu không ignore ở đây, xoá mất giá trị cũ TRƯỚC CẢ khi guard trong service kịp
    // chạy. Bug thật bắt được lúc live-verify: PUT /users/{userId} chỉ gán role (không kèm
    // password) đã âm thầm null hoá password, khoá user khỏi đăng nhập bằng password.
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
