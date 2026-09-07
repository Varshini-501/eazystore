package com.eazybytes.eazystore.service.impl;

import com.eazybytes.eazystore.dto.BulkUploadResultDto;
import com.eazybytes.eazystore.dto.VendorProfileDto;
import com.eazybytes.eazystore.dto.VendorProfileUpdateDto;
import com.eazybytes.eazystore.dto.VendorRegisterRequestDto;
import com.eazybytes.eazystore.entity.Customer;
import com.eazybytes.eazystore.entity.Product;
import com.eazybytes.eazystore.entity.Role;
import com.eazybytes.eazystore.entity.VendorProfile;
import com.eazybytes.eazystore.exception.ResourceNotFoundException;
import com.eazybytes.eazystore.repository.CustomerRepository;
import com.eazybytes.eazystore.repository.PostRepository;
import com.eazybytes.eazystore.repository.ProductRepository;
import com.eazybytes.eazystore.repository.RoleRepository;
import com.eazybytes.eazystore.repository.VendorFollowRepository;
import com.eazybytes.eazystore.repository.VendorProfileRepository;
import com.eazybytes.eazystore.service.IVendorService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements IVendorService {

    private static final int MAX_ROW_ERRORS_TRACKED = 200;

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final VendorFollowRepository vendorFollowRepository;
    private final PostRepository postRepository;
    private final ProductRepository productRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void registerVendor(VendorRegisterRequestDto vendorRegisterRequestDto) {
        customerRepository.findByEmailOrMobileNumber(vendorRegisterRequestDto.getEmail(),
                vendorRegisterRequestDto.getMobileNumber()).ifPresent(existing -> {
            throw new IllegalArgumentException("Email or mobile number is already registered");
        });
        if (vendorProfileRepository.existsByStoreNameIgnoreCase(vendorRegisterRequestDto.getStoreName())) {
            throw new IllegalArgumentException("Store name is already taken, please choose another one");
        }

        Customer customer = new Customer();
        customer.setName(vendorRegisterRequestDto.getName());
        customer.setEmail(vendorRegisterRequestDto.getEmail());
        customer.setMobileNumber(vendorRegisterRequestDto.getMobileNumber());
        customer.setPasswordHash(passwordEncoder.encode(vendorRegisterRequestDto.getPassword()));

        Set<Role> roles = new HashSet<>();
        roleRepository.findByName("ROLE_VENDOR").ifPresent(roles::add);
        roleRepository.findByName("ROLE_USER").ifPresent(roles::add);
        customer.setRoles(roles);
        customer = customerRepository.save(customer);

        VendorProfile vendorProfile = new VendorProfile();
        vendorProfile.setCustomer(customer);
        vendorProfile.setStoreName(vendorRegisterRequestDto.getStoreName());
        vendorProfile.setStoreDescription(vendorRegisterRequestDto.getStoreDescription());
        vendorProfileRepository.save(vendorProfile);
    }

    @Override
    public VendorProfileDto getMyProfile() {
        VendorProfile vendorProfile = getAuthenticatedVendorProfile();
        return transformToDto(vendorProfile, null);
    }

    @Override
    public VendorProfileDto updateMyProfile(VendorProfileUpdateDto vendorProfileUpdateDto) {
        VendorProfile vendorProfile = getAuthenticatedVendorProfile();
        if (!vendorProfile.getStoreName().equalsIgnoreCase(vendorProfileUpdateDto.getStoreName())
                && vendorProfileRepository.existsByStoreNameIgnoreCase(vendorProfileUpdateDto.getStoreName())) {
            throw new IllegalArgumentException("Store name is already taken, please choose another one");
        }
        vendorProfile.setStoreName(vendorProfileUpdateDto.getStoreName());
        vendorProfile.setStoreDescription(vendorProfileUpdateDto.getStoreDescription());
        if (vendorProfileUpdateDto.getLogoUrl() != null) {
            vendorProfile.setLogoUrl(vendorProfileUpdateDto.getLogoUrl());
        }
        vendorProfile = vendorProfileRepository.save(vendorProfile);
        return transformToDto(vendorProfile, null);
    }

    @Override
    public BulkUploadResultDto bulkUploadProducts(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a CSV or Excel file to upload");
        }
        VendorProfile vendorProfile = getAuthenticatedVendorProfile();
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);

        List<ParsedProductRow> rows;
        if (filename.endsWith(".csv")) {
            rows = parseCsv(file);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            rows = parseExcel(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload a .csv, .xlsx or .xls file");
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();
        List<Product> productsToSave = new ArrayList<>();

        for (ParsedProductRow row : rows) {
            String validationError = validateRow(row);
            if (validationError != null) {
                if (errors.size() < MAX_ROW_ERRORS_TRACKED) {
                    errors.add("Row " + row.rowNumber + ": " + validationError);
                }
                continue;
            }
            Product product = new Product();
            product.setName(row.name.trim());
            product.setDescription(row.description.trim());
            product.setPrice(row.price);
            product.setPopularity(row.popularity != null ? row.popularity : 0);
            product.setImageUrl(row.imageUrl != null ? row.imageUrl.trim() : null);
            product.setVendor(vendorProfile);
            productsToSave.add(product);
            successCount++;
        }

        if (!productsToSave.isEmpty()) {
            productRepository.saveAll(productsToSave);
        }

        return new BulkUploadResultDto(rows.size(), successCount, rows.size() - successCount, errors);
    }

    private String validateRow(ParsedProductRow row) {
        if (isBlank(row.name)) {
            return "Product name is required";
        }
        if (row.name.trim().length() > 250) {
            return "Product name must be at most 250 characters";
        }
        if (isBlank(row.description)) {
            return "Product description is required";
        }
        if (row.description.trim().length() > 500) {
            return "Product description must be at most 500 characters";
        }
        if (row.price == null) {
            return "A valid price is required";
        }
        if (row.price.compareTo(BigDecimal.ZERO) <= 0) {
            return "Price must be greater than zero";
        }
        if (row.priceParseError) {
            return "Price could not be read as a number";
        }
        if (row.popularityParseError) {
            return "Popularity could not be read as a whole number";
        }
        return null;
    }

    private List<ParsedProductRow> parseCsv(MultipartFile file) throws IOException {
        List<ParsedProductRow> rows = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreSurroundingSpaces(true)
                    .build();
            CSVParser parser = format.parse(reader);
            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                ParsedProductRow row = new ParsedProductRow();
                row.rowNumber = rowNumber;
                row.name = getCsvValue(record, "name");
                row.description = getCsvValue(record, "description");
                row.imageUrl = getCsvValue(record, "imageUrl", "image_url", "image url");
                row.price = parsePrice(getCsvValue(record, "price"), row);
                row.popularity = parsePopularity(getCsvValue(record, "popularity"), row);
                rows.add(row);
            }
        }
        return rows;
    }

    private String getCsvValue(CSVRecord record, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            if (record.isMapped(header)) {
                return record.get(header);
            }
        }
        return null;
    }

    private List<ParsedProductRow> parseExcel(MultipartFile file) throws IOException {
        List<ParsedProductRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return rows;
            }
            int nameCol = -1, descCol = -1, priceCol = -1, popularityCol = -1, imageCol = -1;
            for (Cell cell : headerRow) {
                String header = getCellString(cell).trim().toLowerCase(Locale.ROOT);
                switch (header) {
                    case "name" -> nameCol = cell.getColumnIndex();
                    case "description" -> descCol = cell.getColumnIndex();
                    case "price" -> priceCol = cell.getColumnIndex();
                    case "popularity" -> popularityCol = cell.getColumnIndex();
                    case "imageurl", "image_url", "image url" -> imageCol = cell.getColumnIndex();
                    default -> {
                    }
                }
            }

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row currentRow = sheet.getRow(rowIndex);
                if (currentRow == null || isRowEmpty(currentRow)) {
                    continue;
                }
                ParsedProductRow row = new ParsedProductRow();
                row.rowNumber = rowIndex + 1;
                row.name = nameCol >= 0 ? getCellString(currentRow.getCell(nameCol)) : null;
                row.description = descCol >= 0 ? getCellString(currentRow.getCell(descCol)) : null;
                row.imageUrl = imageCol >= 0 ? getCellString(currentRow.getCell(imageCol)) : null;
                row.price = parsePrice(priceCol >= 0 ? getCellString(currentRow.getCell(priceCol)) : null, row);
                row.popularity = parsePopularity(
                        popularityCol >= 0 ? getCellString(currentRow.getCell(popularityCol)) : null, row);
                rows.add(row);
            }
        }
        return rows;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && !getCellString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double numericValue = cell.getNumericCellValue();
            if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                return String.valueOf((long) numericValue);
            }
            return String.valueOf(numericValue);
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return String.valueOf(cell.getNumericCellValue());
            } catch (IllegalStateException ex) {
                return cell.getStringCellValue();
            }
        }
        return cell.toString().trim();
    }

    private BigDecimal parsePrice(String rawValue, ParsedProductRow row) {
        if (isBlank(rawValue)) {
            return null;
        }
        try {
            return new BigDecimal(rawValue.trim());
        } catch (NumberFormatException ex) {
            row.priceParseError = true;
            return null;
        }
    }

    private Integer parsePopularity(String rawValue, ParsedProductRow row) {
        if (isBlank(rawValue)) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException ex) {
            row.popularityParseError = true;
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private VendorProfile getAuthenticatedVendorProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return vendorProfileRepository.findByCustomer_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("VendorProfile", "email", email));
    }

    private VendorProfileDto transformToDto(VendorProfile vendorProfile, Long viewingCustomerId) {
        VendorProfileDto dto = new VendorProfileDto();
        dto.setVendorId(vendorProfile.getVendorId());
        dto.setStoreName(vendorProfile.getStoreName());
        dto.setStoreDescription(vendorProfile.getStoreDescription());
        dto.setLogoUrl(vendorProfile.getLogoUrl());
        dto.setFollowerCount(vendorFollowRepository.countByVendor_VendorId(vendorProfile.getVendorId()));
        dto.setPostCount(postRepository.findByVendor_VendorIdOrderByCreatedAtDesc(
                vendorProfile.getVendorId(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        if (viewingCustomerId != null) {
            dto.setFollowingCurrentUser(vendorFollowRepository
                    .existsByVendor_VendorIdAndCustomer_CustomerId(vendorProfile.getVendorId(), viewingCustomerId));
        }
        return dto;
    }

    private static class ParsedProductRow {
        int rowNumber;
        String name;
        String description;
        String imageUrl;
        BigDecimal price;
        Integer popularity;
        boolean priceParseError;
        boolean popularityParseError;
    }
}
