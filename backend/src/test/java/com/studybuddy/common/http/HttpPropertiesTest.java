package com.studybuddy.common.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class HttpPropertiesTest {
    @Test
    void acceptsTheDefaultLimit() {
        assertThat(new HttpProperties(DataSize.ofMegabytes(1)).maxJsonBodySize()).isEqualTo(DataSize.ofMegabytes(1));
    }

    @Test
    void rejectsALimitTheFilterCannotBuffer() {
        assertThatThrownBy(() -> new HttpProperties(DataSize.ofGigabytes(3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpProperties(DataSize.ofBytes(0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpProperties(null)).isInstanceOf(NullPointerException.class);
    }
}
