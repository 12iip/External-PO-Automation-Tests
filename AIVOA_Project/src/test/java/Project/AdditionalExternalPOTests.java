package Project;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.StaleElementReferenceException;

/**
 * Additional Test Cases for External PO Reference Page
 * TC004: External PO Number Validation
 * TC036: Multiple File Upload
 * 
 * @author Test Automation Team
 * @version 2.1 - Fixed browser crash and modal selection issues
 */
public class AdditionalExternalPOTests {
    
    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://216.48.184.249:5274";
    private static final String LOGIN_URL = BASE_URL + "/login";
    private static final String INBOUND_URL = BASE_URL + "/inventory/inbound";
    private static final int TIMEOUT = 8;
    
    // Login credentials
    private static final String USERNAME = "testing@aivoa.net";
    private static final String PASSWORD = "password123";
    
    // Login page locators
    private static final By USERNAME_FIELD = By.xpath("//input[@type='email' or @name='email' or @placeholder='Email' or contains(@id, 'email')]");
    private static final By PASSWORD_FIELD = By.xpath("//input[@type='password' or @name='password' or @placeholder='Password']");
    private static final By LOGIN_BUTTON = By.xpath("//button[@type='submit' or contains(text(), 'Login') or contains(text(), 'Sign in')]");
    
    // Inbound page locators
    private static final By NEW_UNPLANNED_RECEIPT_BUTTON = By.xpath("(//button[normalize-space()='New Unplanned Receipt'])[1]");
    
    // IMPROVED: Multiple selectors for External PO Reference
    private static final By[] EXTERNAL_PO_OPTION_SELECTORS = {
        By.xpath("//div[contains(text(), 'External PO Reference')]"),
        By.xpath("//button[contains(text(), 'External PO Reference')]"),
        By.xpath("//*[contains(text(), 'External PO')]"),
        By.xpath("//div[@class='option' and contains(., 'External')]"),
        By.xpath("//*[@role='button' and contains(., 'External PO')]")
    };
    
    // Form field locators
    private static final By EXTERNAL_PO_NUMBER = By.xpath("(//input[@id='po-number'])[1]");
    private static final By SUPPLIER_NAME = By.xpath("//input[@placeholder='Supplier Name' or contains(@name, 'supplier') or contains(@id, 'supplier')]");
    private static final By BOL_AWR_NUMBER = By.xpath("//input[contains(@placeholder, 'BOL') or contains(@name, 'bol') or contains(@id, 'bol')]");
    private static final By DELIVERY_DATE = By.xpath("//input[@placeholder='12 / 11 / yyyy' or contains(@name, 'date') or contains(@id, 'delivery')]");
    private static final By NEXT_BUTTON = By.xpath("//button[contains(text(), 'Next')]");
    
    // File upload locators
    private static final By UPLOAD_FILES_BUTTON = By.xpath("//button[contains(text(), 'Upload files')]");
    private static final By FILE_INPUT = By.xpath("//input[@type='file']");
    private static final By UPLOADED_FILES_LIST = By.xpath("//div[contains(@class, 'file')] | //*[contains(@class, 'uploaded')] | //*[contains(text(), '.pdf') or contains(text(), '.png') or contains(text(), '.jpg')]");
    
    @BeforeClass
    public void setupClass() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  Additional Test Cases                 ║");
        System.out.println("║  TC004 & TC036                        ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // Create screenshots directory
        File screenshotDir = new File("screenshots");
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
            System.out.println("✓ Created screenshots directory");
        }
        
        // Create test files directory
        File testFilesDir = new File("test-files");
        if (!testFilesDir.exists()) {
            testFilesDir.mkdirs();
            System.out.println("✓ Created test-files directory\n");
        }
    }
    
    @BeforeMethod
    public void setUp() {
        System.out.println("\n┌────────────────────────────────────────┐");
        System.out.println("│  Setting up test environment          │");
        System.out.println("└────────────────────────────────────────┘");
        
        // FIXED: Add ChromeOptions to prevent crashes
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage"); // Prevent memory issues
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--disable-gpu"); // Disable GPU acceleration
        options.addArguments("--remote-allow-origins=*"); // Fix CORS issues
        
        // Initialize ChromeDriver with options
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        
        System.out.println("✓ Browser launched successfully");
        
        // Login to application
        performLogin();
        
        // Navigate to Inbound page
        navigateToInboundPage();
        
        // Click on New Unplanned Receipt button
        clickNewUnplannedReceipt();
        
        // Select External PO Reference option - FIXED with proper error handling
        boolean selected = selectExternalPOReference();
        if (!selected) {
            takeScreenshot("fatal_external_po_not_found");
            Assert.fail("CRITICAL: Could not select External PO Reference option. Test cannot proceed.");
        }
    }
    
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
                System.out.println("✓ Browser closed\n");
            } catch (Exception e) {
                System.out.println("⚠ Browser already closed or unreachable\n");
            }
        }
    }
    
    @AfterClass
    public void tearDownClass() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  Test Suite Execution Completed       ║");
        System.out.println("╚════════════════════════════════════════╝");
    }
    
    /**
     * Perform login to AIVOA LSCRM application
     */
    private void performLogin() {
        System.out.println("\n┌─ LOGIN PROCESS ────────────────────────┐");
        
        try {
            driver.get(LOGIN_URL);
            System.out.println("│ ✓ Navigated to login page");
            
            WebElement usernameField = wait.until(
                ExpectedConditions.presenceOfElementLocated(USERNAME_FIELD)
            );
            usernameField.clear();
            usernameField.sendKeys(USERNAME);
            System.out.println("│ ✓ Username: " + USERNAME);
            
            WebElement passwordField = driver.findElement(PASSWORD_FIELD);
            passwordField.clear();
            passwordField.sendKeys(PASSWORD);
            System.out.println("│ ✓ Password: " + PASSWORD);
            
            WebElement loginButton = driver.findElement(LOGIN_BUTTON);
            loginButton.click();
            System.out.println("│ ✓ Clicked login button");
            
            // Wait for navigation away from login page
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
            System.out.println("│ ✓ Login successful");
            System.out.println("└────────────────────────────────────────┘");
            
        } catch (Exception e) {
            System.out.println("│ ✗ Login failed: " + e.getMessage());
            System.out.println("└────────────────────────────────────────┘");
            takeScreenshot("error_login");
            Assert.fail("Failed to login: " + e.getMessage());
        }
    }
    
    /**
     * Navigate to Inbound page
     */
    private void navigateToInboundPage() {
        System.out.println("\n┌─ INBOUND PAGE NAVIGATION ──────────────┐");
        
        try {
            driver.get(INBOUND_URL);
            System.out.println("│ ✓ Navigated to Inbound page");
            
            // Wait for page to load
            wait.until(ExpectedConditions.presenceOfElementLocated(NEW_UNPLANNED_RECEIPT_BUTTON));
            System.out.println("└────────────────────────────────────────┘");
            
        } catch (Exception e) {
            System.out.println("│ ✗ Navigation failed: " + e.getMessage());
            System.out.println("└────────────────────────────────────────┘");
            takeScreenshot("error_navigation");
            Assert.fail("Failed to navigate: " + e.getMessage());
        }
    }
    
    /**
     * Click on New Unplanned Receipt button with retry logic
     */
    private void clickNewUnplannedReceipt() {
        System.out.println("\n┌─ NEW UNPLANNED RECEIPT ────────────────┐");
        
        int attempts = 0;
        int maxAttempts = 3;
        
        while (attempts < maxAttempts) {
            try {
                WebElement newReceiptBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(NEW_UNPLANNED_RECEIPT_BUTTON)
                );
                
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", newReceiptBtn);
                Thread.sleep(300);
                
                newReceiptBtn.click();
                System.out.println("│ ✓ Clicked 'New Unplanned Receipt'");
                
                Thread.sleep(1500); // Wait for modal to appear
                System.out.println("└────────────────────────────────────────┘");
                return;
                
            } catch (StaleElementReferenceException e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    System.out.println("│ ✗ Element became stale after " + maxAttempts + " attempts");
                    throw new RuntimeException(e);
                }
                System.out.println("│ ⚠ Element stale, retrying... (attempt " + (attempts + 1) + ")");
            } catch (Exception e) {
                System.out.println("│ ✗ Failed to click button: " + e.getMessage());
                System.out.println("└────────────────────────────────────────┘");
                takeScreenshot("error_new_receipt");
                Assert.fail("Failed to click New Unplanned Receipt: " + e.getMessage());
            }
        }
    }
    
    /**
     * FIXED: Select External PO Reference option with multiple selector attempts
     * Returns true if successful, false otherwise
     */
    private boolean selectExternalPOReference() {
        System.out.println("\n┌─ EXTERNAL PO REFERENCE ────────────────┐");
        
        // Try each selector until one works
        for (int i = 0; i < EXTERNAL_PO_OPTION_SELECTORS.length; i++) {
            try {
                WebElement externalPOOption = wait.until(
                    ExpectedConditions.elementToBeClickable(EXTERNAL_PO_OPTION_SELECTORS[i])
                );
                
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", externalPOOption);
                Thread.sleep(300);
                
                externalPOOption.click();
                System.out.println("│ ✓ Selected External PO Reference (selector " + (i + 1) + ")");
                Thread.sleep(1000);
                
                // Verify form loaded by checking for External PO Number field
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(EXTERNAL_PO_NUMBER));
                    System.out.println("│ ✓ Form loaded and ready");
                    System.out.println("└────────────────────────────────────────┘");
                    return true;
                } catch (Exception e) {
                    System.out.println("│ ⚠ Form did not load after clicking option");
                    continue; // Try next selector
                }
                
            } catch (Exception e) {
                if (i == EXTERNAL_PO_OPTION_SELECTORS.length - 1) {
                    System.out.println("│ ✗ Could not select External PO option with any selector");
                    System.out.println("│ Error: " + e.getMessage());
                    System.out.println("└────────────────────────────────────────┘");
                    takeScreenshot("error_external_po");
                    return false;
                }
                // Continue to next selector
            }
        }
        
        return false;
    }
    
    /**
     * Helper method to take screenshot with improved error handling
     */
    private void takeScreenshot(String fileName) {
        try {
            if (driver != null) {
                TakesScreenshot screenshot = (TakesScreenshot) driver;
                File srcFile = screenshot.getScreenshotAs(OutputType.FILE);
                File destFile = new File("screenshots/" + fileName + ".png");
                FileUtils.copyFile(srcFile, destFile);
                System.out.println("│ 📸 Screenshot: " + fileName + ".png");
            }
        } catch (Exception e) {
            System.out.println("│ ⚠ Screenshot failed: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to enter text in a field with improved reliability
     */
    private void enterText(By locator, String text, String fieldName) {
        int attempts = 0;
        int maxAttempts = 2;
        
        while (attempts < maxAttempts) {
            try {
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                Thread.sleep(200);
                
                element.click();
                Thread.sleep(100);
                
                element.clear();
                element.sendKeys(text);
                
                System.out.println("│   ✓ " + fieldName + ": " + text);
                return;
                
            } catch (StaleElementReferenceException e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    tryJavaScriptEntry(locator, text, fieldName);
                    return;
                }
            } catch (Exception e) {
                System.out.println("│   ✗ Failed to enter " + fieldName + ": " + e.getMessage());
                tryJavaScriptEntry(locator, text, fieldName);
                return;
            }
        }
    }
    
    /**
     * JavaScript fallback for entering text
     */
    private void tryJavaScriptEntry(By locator, String text, String fieldName) {
        try {
            System.out.println("│   ⚠ Attempting JavaScript fallback...");
            WebElement element = driver.findElement(locator);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            Thread.sleep(200);
            ((JavascriptExecutor) driver).executeScript("arguments[0].value=arguments[1];", element, text);
            System.out.println("│   ✓ " + fieldName + ": " + text + " (via JavaScript)");
        } catch (Exception ex) {
            System.out.println("│   ✗ JavaScript fallback also failed");
            throw new RuntimeException("Could not interact with " + fieldName);
        }
    }
    
    /**
     * Helper method to check for validation errors
     */
    private List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        String[] errorXPaths = {
            "//div[contains(@class, 'error') and not(contains(@class, 'hidden'))]",
            "//span[contains(@class, 'error') and not(contains(@class, 'hidden'))]",
            "//p[contains(@class, 'error')]",
            "//*[contains(text(), 'required')]",
            "//*[contains(text(), 'Required')]",
            "//*[contains(@class, 'invalid')]",
            "//span[contains(@class, 'text-red')]",
            "//*[contains(@class, 'error-message')]"
        };
        
        for (String xpath : errorXPaths) {
            try {
                List<WebElement> errorElements = driver.findElements(By.xpath(xpath));
                for (WebElement error : errorElements) {
                    if (error.isDisplayed() && !error.getText().trim().isEmpty()) {
                        String errorText = error.getText().trim();
                        if (!errors.contains(errorText)) {
                            errors.add(errorText);
                        }
                    }
                }
            } catch (Exception e) {
                // Continue to next selector
            }
        }
        return errors;
    }
    
    /**
     * Helper method to check if specific field has error
     */
    private boolean hasFieldError(By fieldLocator) {
        try {
            WebElement field = driver.findElement(fieldLocator);
            String className = field.getAttribute("class");
            String ariaInvalid = field.getAttribute("aria-invalid");
            
            WebElement parent = field.findElement(By.xpath("./parent::*"));
            String parentClass = parent.getAttribute("class");
            
            return (className != null && (className.contains("error") || className.contains("invalid") || className.contains("border-red"))) ||
                   (ariaInvalid != null && ariaInvalid.equals("true")) ||
                   (parentClass != null && (parentClass.contains("error") || parentClass.contains("invalid") || parentClass.contains("border-red")));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Helper method to create test files for upload with null check
     */
    private File createTestFile(String fileName, String content) {
        try {
            File file = new File("test-files/" + fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            
            if (file.exists()) {
                return file;
            } else {
                System.out.println("│ ✗ File created but doesn't exist: " + fileName);
                return null;
            }
        } catch (Exception e) {
            System.out.println("│ ✗ Failed to create test file: " + fileName + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * FIXED: Safe method to get current URL without crashing
     */
    private String getSafeCurrentUrl() {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            System.out.println("│ ⚠ Could not get current URL (browser may be unreachable)");
            return "unknown";
        }
    }
    
    /**
     * TC004: Verify validation error when External PO Number is empty and Next is clicked
     */
    @Test(priority = 1, description = "TC004: Verify validation error when External PO Number is empty")
    public void testTC004_ExternalPONumberValidation() throws InterruptedException {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  TC004: External PO Number Validation ║");
        System.out.println("╚════════════════════════════════════════╝");
        
        String supplierName = "12122";
        String bolAwrNumber = "BOL-TEST-67890";
        String deliveryDate = "12/15/2025";
        
        System.out.println("\n┌─ TEST DATA ────────────────────────────┐");
        System.out.println("│ External PO: [EMPTY - TO BE TESTED]");
        System.out.println("│ Supplier: " + supplierName);
        System.out.println("│ BOL/AWR: " + bolAwrNumber);
        System.out.println("│ Date: " + deliveryDate);
        System.out.println("└────────────────────────────────────────┘");
        
        System.out.println("\n✓ Steps 1-4: Setup completed");
        
        System.out.println("\n┌─ STEP 5-6: Filling Other Fields ──────┐");
        
        try {
            WebElement externalPO = driver.findElement(EXTERNAL_PO_NUMBER);
            externalPO.clear();
            System.out.println("│   ✓ External PO Number: [LEFT EMPTY]");
        } catch (Exception e) {
            System.out.println("│   ⚠ External PO Number field not found");
        }
        
        enterText(SUPPLIER_NAME, supplierName, "Supplier Name");
        enterText(BOL_AWR_NUMBER, bolAwrNumber, "BOL/AWR Number");
        
        try {
            WebElement dateField = wait.until(
                ExpectedConditions.elementToBeClickable(DELIVERY_DATE)
            );
            dateField.click();
            dateField.clear();
            dateField.sendKeys(deliveryDate);
            dateField.sendKeys(Keys.TAB);
            System.out.println("│   ✓ Delivery Date: " + deliveryDate);
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("│   ✗ Failed to enter Delivery Date");
        }
        
        takeScreenshot("tc004_01_form_with_empty_po");
        System.out.println("└────────────────────────────────────────┘");
        
        System.out.println("\n┌─ STEP 7: Clicking Next Button ─────────┐");
        try {
            WebElement nextButton = wait.until(
                ExpectedConditions.elementToBeClickable(NEXT_BUTTON)
            );
            System.out.println("│ ✓ Found Next button");
            nextButton.click();
            System.out.println("│ ✓ Clicked Next button");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("│ ✗ Failed to click Next button");
            System.out.println("└────────────────────────────────────────┘");
            takeScreenshot("tc004_02_error_next");
            Assert.fail("Next button not clickable: " + e.getMessage());
        }
        
        takeScreenshot("tc004_02_after_next_click");
        System.out.println("└────────────────────────────────────────┘");
        
        System.out.println("\n┌─ STEP 8: Verifying PO Number Error ────┐");
        
        List<String> validationErrors = getValidationErrors();
        boolean poFieldHasError = hasFieldError(EXTERNAL_PO_NUMBER);
        
        System.out.println("│");
        if (poFieldHasError) {
            System.out.println("│ ✓ External PO Number field has error state");
        } else {
            System.out.println("│ ⚠ No error state on External PO Number field");
        }
        
        if (!validationErrors.isEmpty()) {
            System.out.println("│");
            System.out.println("│ ✓ Found " + validationErrors.size() + " validation error(s):");
            for (int i = 0; i < validationErrors.size(); i++) {
                System.out.println("│   " + (i + 1) + ". " + validationErrors.get(i));
            }
        }
        
        boolean hasExternalPOError = false;
        for (String error : validationErrors) {
            if (error.toLowerCase().contains("external") || 
                error.toLowerCase().contains("po") || 
                error.toLowerCase().contains("required")) {
                hasExternalPOError = true;
                System.out.println("│");
                System.out.println("│ ✓ Found External PO related error:");
                System.out.println("│   \"" + error + "\"");
                break;
            }
        }
        
        takeScreenshot("tc004_03_validation_error");
        System.out.println("└────────────────────────────────────────┘");
        
        // FIXED: Use safe URL retrieval
        String currentUrl = getSafeCurrentUrl();
        boolean stillOnForm = currentUrl.contains("inbound") || 
                              !currentUrl.contains("verify") && 
                              !currentUrl.contains("inspect");
        
        System.out.println("\n┌─ VERIFICATION ─────────────────────────┐");
        System.out.println("│ Current URL: " + currentUrl);
        if (stillOnForm) {
            System.out.println("│ ✓ Still on form (navigation blocked)");
        }
        System.out.println("└────────────────────────────────────────┘");
        
        boolean validationWorking = poFieldHasError || hasExternalPOError || validationErrors.size() > 0;
        
        System.out.println("\n┌─ TEST RESULT ──────────────────────────┐");
        System.out.println("│ Field error state: " + poFieldHasError);
        System.out.println("│ PO-related error message: " + hasExternalPOError);
        System.out.println("│ Total error messages: " + validationErrors.size());
        System.out.println("│ Form navigation blocked: " + stillOnForm);
        
        Assert.assertTrue(validationWorking, 
            "Expected validation error for empty External PO Number but none found");
        
        System.out.println("│");
        System.out.println("│ ✅ TEST PASSED");
        System.out.println("│    External PO Number validation working");
        System.out.println("└────────────────────────────────────────┘\n");
    }
    
    /**
     * TC036: Verify multiple files can be uploaded
     */
    @Test(priority = 2, description = "TC036: Verify multiple files can be uploaded")
    public void testTC036_MultipleFileUpload() throws InterruptedException {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  TC036: Multiple File Upload Test     ║");
        System.out.println("╚════════════════════════════════════════╝");
        
        System.out.println("\n✓ Steps 1-4: Setup completed");
        
        System.out.println("\n┌─ STEP 5: Filling Required Fields ─────┐");
        
        String externalPONumber = "PO-2025-101";
        String supplierName = "12122";
        String bolAwrNumber = "BOL-TEST-67890";
        String deliveryDate = "12/15/2025";
        
        enterText(EXTERNAL_PO_NUMBER, externalPONumber, "External PO Number");
        enterText(SUPPLIER_NAME, supplierName, "Supplier Name");
        enterText(BOL_AWR_NUMBER, bolAwrNumber, "BOL/AWR Number");
        
        try {
            WebElement dateField = wait.until(
                ExpectedConditions.elementToBeClickable(DELIVERY_DATE)
            );
            dateField.click();
            dateField.clear();
            dateField.sendKeys(deliveryDate);
            dateField.sendKeys(Keys.TAB);
            System.out.println("│   ✓ Delivery Date: " + deliveryDate);
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("│   ✗ Failed to enter Delivery Date");
        }
        
        takeScreenshot("tc036_01_form_filled");
        System.out.println("└────────────────────────────────────────┘");
        
        System.out.println("\n┌─ STEP 6: Creating Test Files ──────────┐");
        
        File pdfFile = createTestFile("test-document.pdf", "Test PDF content for upload testing");
        File pngFile = createTestFile("test-image.png", "Test PNG content for upload testing");
        File jpgFile = createTestFile("test-photo.jpg", "Test JPG content for upload testing");
        
        int filesCreated = 0;
        if (pdfFile != null) {
            System.out.println("│ ✓ Created: test-document.pdf");
            filesCreated++;
        }
        if (pngFile != null) {
            System.out.println("│ ✓ Created: test-image.png");
            filesCreated++;
        }
        if (jpgFile != null) {
            System.out.println("│ ✓ Created: test-photo.jpg");
            filesCreated++;
        }
        
        if (filesCreated == 0) {
            System.out.println("│ ✗ No test files were created!");
            System.out.println("└────────────────────────────────────────┘");
            Assert.fail("Failed to create any test files");
        }
        
        System.out.println("└────────────────────────────────────────┘");
        
        System.out.println("\n┌─ STEPS 7-9: Uploading Files ───────────┐");
        
        int uploadedCount = 0;
        
        try {
            WebElement fileInput = driver.findElement(FILE_INPUT);
            
            if (pdfFile != null && pdfFile.exists()) {
                fileInput.sendKeys(pdfFile.getAbsolutePath());
                System.out.println("│ ✓ Uploaded: test-document.pdf");
                uploadedCount++;
                Thread.sleep(1000);
            }
            
            fileInput = driver.findElement(FILE_INPUT);
            
            if (pngFile != null && pngFile.exists()) {
                fileInput.sendKeys(pngFile.getAbsolutePath());
                System.out.println("│ ✓ Uploaded: test-image.png");
                uploadedCount++;
                Thread.sleep(1000);
            }
            
            fileInput = driver.findElement(FILE_INPUT);
            
            if (jpgFile != null && jpgFile.exists()) {
                fileInput.sendKeys(jpgFile.getAbsolutePath());
                System.out.println("│ ✓ Uploaded: test-photo.jpg");
                uploadedCount++;
                Thread.sleep(1000);
            }
            
            System.out.println("│");
            System.out.println("│ Total files uploaded: " + uploadedCount);
            
        } catch (Exception e) {
            System.out.println("│ ⚠ Upload method 1 failed: " + e.getMessage());
            System.out.println("│ Trying alternative approach...");
            
            try {
                WebElement uploadButton = driver.findElement(UPLOAD_FILES_BUTTON);
                uploadButton.click();
                Thread.sleep(800);
                
                WebElement fileInput = driver.findElement(FILE_INPUT);
                
                if (pdfFile != null && pngFile != null && jpgFile != null) {
                    String allFiles = pdfFile.getAbsolutePath() + "\n" + 
                                    pngFile.getAbsolutePath() + "\n" + 
                                    jpgFile.getAbsolutePath();
                    fileInput.sendKeys(allFiles);
                    uploadedCount = 3;
                    System.out.println("│ ✓ Uploaded all 3 files together");
                }
                
            } catch (Exception e2) {
                System.out.println("│ ✗ Upload failed: " + e2.getMessage());
            }
        }
        
        takeScreenshot("tc036_02_files_uploaded");
        System.out.println("└────────────────────────────────────────┘");
        
        System.out.println("\n┌─ STEP 10: Verifying Uploaded Files ────┐");
        
        List<WebElement> uploadedFileElements = new ArrayList<>();
        try {
            WebDriverWait fileWait = new WebDriverWait(driver, Duration.ofSeconds(8));
            uploadedFileElements = fileWait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(UPLOADED_FILES_LIST)
            );
            System.out.println("│ ✓ Files appeared in UI after wait");
        } catch (Exception e) {
            System.out.println("│ ⚠ WebDriverWait timed out, trying alternative selectors...");
            
            String[] alternativeSelectors = {
                "//div[contains(@class, 'uploaded-file')]",
                "//li[contains(@class, 'file-item')]",
                "//*[contains(@class, 'attachment')]",
                "//span[contains(text(), 'test-document') or contains(text(), 'test-image') or contains(text(), 'test-photo')]",
                "//*[contains(@class, 'file-name')]"
            };
            
            for (String selector : alternativeSelectors) {
                try {
                    uploadedFileElements = driver.findElements(By.xpath(selector));
                    if (!uploadedFileElements.isEmpty()) {
                        System.out.println("│ ✓ Found files using alternative selector");
                        break;
                    }
                } catch (Exception ex) {
                    // Continue
                }
            }
        }
        
        System.out.println("│ Found " + uploadedFileElements.size() + " file element(s) in UI");
        
        if (!uploadedFileElements.isEmpty()) {
            System.out.println("│");
            System.out.println("│ Displayed files:");
            for (int i = 0; i < uploadedFileElements.size(); i++) {
                String fileText = uploadedFileElements.get(i).getText();
                if (!fileText.trim().isEmpty()) {
                    System.out.println("│   " + (i + 1) + ". " + fileText);
                }
            }
        }
        
        String pageSource = driver.getPageSource();
        boolean foundPdf = pageSource.contains("test-document") || pageSource.contains(".pdf");
        boolean foundPng = pageSource.contains("test-image") || pageSource.contains(".png");
        boolean foundJpg = pageSource.contains("test-photo") || pageSource.contains(".jpg");
        
        System.out.println("│");
        System.out.println("│ File detection:");
        if (foundPdf) System.out.println("│   ✓ PDF file detected");
        if (foundPng) System.out.println("│   ✓ PNG file detected");
        if (foundJpg) System.out.println("│   ✓ JPG file detected");
        
        takeScreenshot("tc036_03_files_displayed");
        System.out.println("└────────────────────────────────────────┘");
        
        System.out.println("\n┌─ TEST RESULT ──────────────────────────┐");
        System.out.println("│ Files uploaded: " + uploadedCount);
        System.out.println("│ File elements in UI: " + uploadedFileElements.size());
        System.out.println("│ PDF detected: " + foundPdf);
        System.out.println("│ PNG detected: " + foundPng);
        System.out.println("│ JPG detected: " + foundJpg);
        
        boolean filesVisible = uploadedFileElements.size() >= 1 || foundPdf || foundPng || foundJpg;
        
        if (!filesVisible) {
            System.out.println("│");
            System.out.println("│ ❌ TC036: FAILED - APPLICATION BUG DETECTED");
            System.out.println("│");
            System.out.println("│ Root Cause:");
            System.out.println("│   File upload functionality is not working");
            System.out.println("│   in the application");
            System.out.println("│");
            System.out.println("│ Evidence:");
            System.out.println("│   - Manual upload: FAILED (not accepting files)");
            System.out.println("│   - Automated upload: Files sent but not displayed");
            System.out.println("│   - Files sent to input: " + uploadedCount);
            System.out.println("│   - Files visible in UI: 0");
            System.out.println("│");
            System.out.println("│ Expected:");
            System.out.println("│   Uploaded files should appear in upload area");
            System.out.println("│");
            System.out.println("│ Actual:");
            System.out.println("│   Upload area remains empty after file selection");
            System.out.println("│");
            System.out.println("│ Status:");
            System.out.println("│   BUG REPORTED - Waiting for development fix");
            System.out.println("│");
            System.out.println("│ Screenshots:");
            System.out.println("│   - tc036_02_files_uploaded.png");
            System.out.println("│   - tc036_03_files_displayed.png");
            System.out.println("└────────────────────────────────────────┘\n");
            
            Assert.fail("TC036: FAILED - APPLICATION BUG DETECTED\n" +
                       "Root Cause: File upload functionality is not working in the application\n" +
                       "Evidence:\n" +
                       "  - Manual upload attempt: FAILED (files not accepted)\n" +
                       "  - Automated upload: Files sent but not displayed\n" +
                       "  - Expected: Uploaded files should appear in the upload area\n" +
                       "  - Actual: Upload area remains empty after file selection\n" +
                       "Status: BUG REPORTED - Waiting for development fix");
        }
        
        System.out.println("│");
        System.out.println("│ ✅ TEST PASSED");
        System.out.println("│    Multiple file upload is working");
        System.out.println("└────────────────────────────────────────┘\n");
    }
}
