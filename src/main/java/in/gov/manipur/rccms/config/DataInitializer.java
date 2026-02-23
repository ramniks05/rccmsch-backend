package in.gov.manipur.rccms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data Initializer
 * NOTE: This initializer has been disabled for fresh Chandigarh implementation.
 * All master data (Case Natures, Admin Units, etc.) should be configured via admin APIs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("Data Initializer - Fresh Start for Chandigarh");
        log.info("========================================");
        log.info("All master data (Case Natures, Admin Units, etc.) should be configured via admin APIs");
        log.info("No automatic data initialization - ready for Chandigarh implementation");
        log.info("========================================");
    }

}

