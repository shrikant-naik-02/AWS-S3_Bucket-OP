package com.excelfore.aws.awstask.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUrlResponse {
    private String type;
    private String url;
    private String expiration;
    private boolean valid;

}
