package com.final_team4.finalbe.setting.domain.uploadChannel;

import lombok.Getter;

@Getter
public enum Channel {
    X("X"),
    INSTAGRAM("인스타그램"),
    NAVER("네이버 블로그");

    private final String description;

    Channel(String description) {
        this.description = description;
    }

}
