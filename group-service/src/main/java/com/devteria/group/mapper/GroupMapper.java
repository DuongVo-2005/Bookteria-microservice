package com.devteria.group.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.devteria.group.dto.GroupMemberRole;
import com.devteria.group.dto.response.GroupResponse;
import com.devteria.group.entity.Group;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    @Mapping(target = "memberCount", source = "memberCount")
    @Mapping(target = "joined", source = "joined")
    @Mapping(target = "currentUserRole", source = "currentUserRole")
    GroupResponse toGroupResponse(Group group, long memberCount, boolean joined, GroupMemberRole currentUserRole);
}
