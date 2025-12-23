# Compiler and flags
CC = riscv64-unknown-elf-gcc
CFLAGS = -fno-common -fno-builtin-printf -specs=htif_nano.specs
LDFLAGS = -static -specs=htif_nano.specs

# File extensions
TARGET_EXT = .riscv

# Find all C source files in the current directory
SRCS = $(wildcard *.c)
# Generate the list of object files from the source files
OBJS = $(patsubst %.c,%.o,$(SRCS))
# Generate the list of target executables
TARGETS = $(patsubst %.c,%$(TARGET_EXT),$(SRCS))

.PHONY: all clean

# Default rule: build all targets
all: $(TARGETS)

# Rule to link an object file into a RISC-V executable
%$(TARGET_EXT): %.o
	$(CC) $(LDFLAGS) $< -o $@

# Rule to compile a C source file into an object file
%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

# Rule to clean up generated files
clean:
	rm -f $(TARGETS) $(OBJS)