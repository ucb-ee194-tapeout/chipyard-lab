# [Optional] More Complicated Configs & Tests

Complete this section if you want to see some more complicated systems. Navigate to `$chipyard/generators/chipyard/src/main/scala/config/TutorialConfigs.scala`. We'll be running the `CONFIG=TutorialNoCConfig` config whichs adds one of the aforementioned Constellation topologies into our system. Run

```sh
srun -p ee194 --pty make CONFIG=TutorialNoCConfig -j16
```
and inspect the generated files at `$chipyard/sims/vcs/generated-src`

To run some more interesting tests, first, go to `$chipyard/tests` and run the following commands.
```sh
cmake -S ./ -B ./build/ -D CMAKE_BUILD_TYPE=Debug
cmake --build ./build/ --target all
```
The exact semantics of these commands aren't too important now, but they tell [CMake](https://cmake.org/), a build system to setup a project with debug symbols included and to build all of the tests. CMake is especially helpful when managing large code bases and is one of the leading tools for C/C++ build management.

> *Note: if you are wondering, the `.riscv` binaries are actually [ELF files](https://en.wikipedia.org/wiki/Executable_and_Linkable_Format). We are naming it with the .riscv extension to emphasize that it is a RISC-V program.*


Afterwards, you should see the `.riscv` bare-metal binaries compiled in the new build folder along with a bunch of `.dump` files which contain the corresponding disassemblies. Go back to `$chipyard/sims/vcs` and try running:
- `make CONFIG=TutorialNoCConfig run-binary-hex BINARY=../../tests/build/fft.riscv` Runs tests on the FFT accelerator that's connected through a MMIO.
- `make CONFIG=TutorialNoCConfig run-binary-hex BINARY=../../tests/build/gcd.riscv` Runs tests on a GCD module that's connected through a MMIO.
- `make CONFIG=TutorialNoCConfig run-binary-hex BINARY=../../tests/build/streaming-fir.riscv` Runs [FIR](https://en.wikipedia.org/wiki/Finite_impulse_response) tests.
- `make CONFIG=TutorialNoCConfig run-binary-hex BINARY=../../tests/build/nic-loopback.riscv` Runs test on the [NiC](https://en.wikipedia.org/wiki/Network_interface_controller) tests.

See also: https://chipyard.readthedocs.io/en/stable/Simulation/Software-RTL-Simulation.html#fast-memory-loading