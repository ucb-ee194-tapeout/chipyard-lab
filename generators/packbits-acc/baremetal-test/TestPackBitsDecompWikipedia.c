#include "../../../tests/rocc.h"
#include <inttypes.h>
#include <stdio.h>

int main(void) {

    // Bytes (little-endian in memory):
    // FE AA 02 80 00 2A FD AA 03 80 00 2A 22 F7 AA 00
    __attribute__((aligned(32))) volatile uint64_t input_1[4] = {
        0xAAFD2A008002AAFEULL, // FE AA 02 80 00 2A FD AA
        0x00AAF7222A008003ULL, // 03 80 00 2A 22 F7 AA 00
        0x0000000000000000ULL,
        0x0000000000000000ULL
    };
    
    volatile uint8_t outbuf[4096] __attribute__((aligned(64)));
    for (int i = 0; i < 4096; i++) {
        outbuf[i] = 0;
    }

    // Run Tests
    printf("Starting PackBits Decompression Test 1 -- Wikipedia Demo: \n");
    printf("(Reading from) Source Address: %p\n", input_1);
    printf("Destination Address: %p\n", outbuf);

    asm volatile("fence");

    // rs2/length = 32 bytes (we only use first 16 bytes as PackBits stream)
    ROCC_INSTRUCTION_SS(0, input_1, 32, 0x1); // Set source address
    ROCC_INSTRUCTION_S(0, outbuf, 0x2);       // Set dest address

    asm volatile("fence");

    // Expected output bytes:
    // AA AA AA 80 00 2A AA AA
    // AA AA 80 00 2A 22 AA AA
    // AA AA AA AA AA AA AA AA
    // 00 00 00 00 00 00 00 00  (pad to 32 bytes for printing)
    uint8_t expect_1[32] = {
        0xAA, 0xAA, 0xAA, 0x80, 0x00, 0x2A, 0xAA, 0xAA,
        0xAA, 0xAA, 0x80, 0x00, 0x2A, 0x22, 0xAA, 0xAA,
        0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    // print outbuf & check correctness
    printf("Decompressed Output\n");
    for (int i = 0; i < 32; i++) {
        printf("%02X ", outbuf[i]);
        if ((i + 1) % 8 == 0) {
            printf("\n");
        }
        // Check correctness
        if (outbuf[i] != expect_1[i]) {
            printf("Test 1 Failed at byte %d: Expected %02X, Got %02X\n",
                   i, expect_1[i], outbuf[i]);
            return -1;
        }
    }

    printf("**PASSED** Test 1 Completed.\n\n");
    return 0;
}
