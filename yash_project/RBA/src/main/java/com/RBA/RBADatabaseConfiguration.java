package com.RBA;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class RBADatabaseConfiguration {
    private String jdbcUrl;
    private String driverClass;
    private String userName;
    private String password;
}
