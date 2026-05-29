package com.niko.Service;

import java.io.InputStream;

public interface DoFilterExcelService {
    void filterExcel(InputStream FileinputStream, String writeFileName);
}
