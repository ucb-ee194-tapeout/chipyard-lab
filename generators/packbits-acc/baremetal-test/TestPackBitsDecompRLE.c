#include "../../../tests/rocc.h"
#include <inttypes.h>
#include <stdio.h>

int main(void) {

    uint64_t input_2[4] = { 0x000000000000AAF7, 0x0, 0x0, 0x0 };
    
    volatile uint8_t outbuf[4096] __attribute__((aligned(64)));
    for (int i = 0; i < 4096; i++) {
        outbuf[i] = 0;
    }

    // Run Tests
    printf("Starting PackBits Decompression Test 2 -- RLE Expansion: \n");
    printf("(Reading from) Source Address: %p\n", input_2);
    printf("Destination Address: %p\n", outbuf);

    asm volatile("fence");

    // rs2/length = 32 bytes
    ROCC_INSTRUCTION_SS(0, input_2, 32, 0x1); // Set source address
    ROCC_INSTRUCTION_S(0, outbuf, 0x2); // Set dest address
    // ROCC_INSTRUCTION_S(0, (uintptr_t)(outbuf+32), 0x2); // Set dest address

    asm volatile("fence");

    uint8_t expect_2[32] = {
        // word 0: 0xAAAAAAAAAAAAAAAA
        0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA,

        // word 1: 0x000000000000AAAA
        0xAA, 0xAA, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

        // word 2: 0x0
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

        // word 3: 0x0
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    // print outbuf & check correctness
    printf("Decompressed Output:\n");
    for (int i = 0; i < 32; i++) {
        printf("%02X ", outbuf[i]);
        if ((i + 1) % 8 == 0) {
            printf("\n");
        }
        // Check correctness
        if (outbuf[i] != expect_2[i]) {
            printf("Test 2 Failed at byte %d: Expected %02X, Got %02X\n", i, expect_2[i], outbuf[i]);
            return -1;
        }
    }

    printf("**PASSED** Test 2 Completed.\n\n");
    return 0;
}