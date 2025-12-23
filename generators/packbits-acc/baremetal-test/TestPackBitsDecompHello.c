#include "../../../tests/rocc.h"
#include <inttypes.h>
#include <stdio.h>

int main(void) {

    uint64_t input_0[4] = { 0x00004F4C4C454804, 0x0, 0x0, 0x0 };
    
    volatile uint8_t outbuf[4096] __attribute__((aligned(64)));
    for (int i = 0; i < 4096; i++) {
        outbuf[i] = 0;
    }

    // Run Tests
    printf("Starting PackBits Decompression Test 0 -- HELLO: \n");
    printf("(Reading from) Source Address: %p\n", input_0);
    printf("Destination Address: %p\n", outbuf);

    asm volatile("fence");

    // rs2/length = 32 bytes
    ROCC_INSTRUCTION_SS(0, input_0, 32, 0x1); // Set source address
    ROCC_INSTRUCTION_S(0, outbuf, 0x2); // Set dest address
    // ROCC_INSTRUCTION_S(0, (uintptr_t)(outbuf+32), 0x2); // Set dest address

    asm volatile("fence");

    uint8_t expect_0[32] = {
        // word 0: 0x00004F4C4C4548ULL
        0x48, 0x45, 0x4C, 0x4C, 0x4F, 0x00, 0x00, 0x00,

        // word 1: 0
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

        // word 2: 0
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

        // word 3: 0
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    // print outbuf & check correctness
    printf("Decompressed Output:\n");
    for (int i = 0; i < 32; i++) {
        printf("%02X ", outbuf[i]);
        if ((i + 1) % 8 == 0) {
            printf("\n");
        }r
        // Check correctness
        if (outbuf[i] != expect_0[i]) {
            printf("Test 0 Failed at byte %d: Expected %02X, Got %02X\n", i, expect_0[i], outbuf[i]);
            return -1;
        }
    }

    printf("**PASSED** Test 0 Completed.\n\n");
    return 0;
}