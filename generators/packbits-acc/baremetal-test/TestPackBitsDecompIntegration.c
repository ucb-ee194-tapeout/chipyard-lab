#include "../../../tests/rocc.h"
#include <inttypes.h>
#include <stdio.h>

int main(void) {

    __attribute__((aligned(32))) volatile uint64_t packbits_img_words[8] = {
        0x0100FDFF000100F9ULL,  // bytes 0–7
        0x0100FDFF000100FFULL,  // bytes 8–15
        0xFF00000700F900FFULL,  // bytes 16–23
        0x0100FE0000FF0000ULL,  // bytes 24–31
        0x00F900F900FEFFFFULL,  // bytes 32–39
        0x0000000000000000ULL,  // bytes 40–47
        0x0000000000000000ULL,  // bytes 48–55
        0x0000000000000000ULL   // bytes 56–63
    };

    volatile uint8_t outbuf[4096] __attribute__((aligned(64)));
    for (int i = 0; i < 4096; i++) {
        outbuf[i] = 0;
    }

    // Run Tests
    printf("Starting PackBits Decompression Test 4 -- Integration: \n");
    printf("(Reading from) Source Address: %p\n", packbits_img_words);
    printf("Destination Address: %p\n", outbuf);

    asm volatile("fence");

    // rs2/length = 64 bytes
    ROCC_INSTRUCTION_SS(0, packbits_img_words, 64, 0x1); // Set source address
    ROCC_INSTRUCTION_S(0, outbuf, 0x2); // Set dest address
    ROCC_INSTRUCTION_S(0, (uintptr_t)(outbuf+32), 0x2); // Set dest address

    asm volatile("fence");

    uint8_t expect_img[64] = {
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0xFF,0x00,0x00,0x00,0x00,0xFF,0x00,
        0x00,0xFF,0x00,0x00,0x00,0x00,0xFF,0x00,
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,

        0x00,0x00,0xFF,0x00,0x00,0xFF,0x00,0x00,
        0x00,0x00,0x00,0xFF,0xFF,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
    };


    // print outbuf & check correctness
    printf("Decompressed Output:\n");
    for (int i = 0; i < 64; i++) {
        printf("%02X ", outbuf[i]);
        if ((i + 1) % 8 == 0) {
            printf("\n");
        }
        // Check correctness
        if (outbuf[i] != expect_img[i]) {
            printf("Test 5 Failed at byte %d: Expected %02X, Got %02X\n", i, expect_img[i], outbuf[i]);
            return -1;
        }
    }
    
    printf("**PASSED** Test 5 Completed.\n\n");
    return 0;
}