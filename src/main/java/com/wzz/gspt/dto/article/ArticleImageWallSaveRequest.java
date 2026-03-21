package com.wzz.gspt.dto.article;

import lombok.Data;

import java.util.List;

@Data
public class ArticleImageWallSaveRequest {

    private Long articleId;

    private List<ArticleImageItemRequest> images;
}
