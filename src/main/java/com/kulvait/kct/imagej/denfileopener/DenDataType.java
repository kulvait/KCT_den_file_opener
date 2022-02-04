/*******************************************************************************
 * Project : KCT ImageJ plugin to open DEN files
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Enum to describe different data types to be stored
 * Date: 2022
 ******************************************************************************/
package com.kulvait.kct.imagej.denfileopener;

public enum DenDataType {
    // Ordinal num is the position in the enum
    // thus for this enum is the ordinal important as its the code in DEX file
    UINT16(2), // 0
    INT16(2), // 1
    UINT32(4), // 2
    INT32(4), // 3
    UINT64(8), // 4
    INT64(8), // 5
    FLOAT32(4), // 6
    FLOAT64(8), // 7
    UINT8(1); // 8

    private final int byteSize;
    private DenDataType(int byteSize) { this.byteSize = byteSize; }
    public int getSize() { return byteSize; }
}
