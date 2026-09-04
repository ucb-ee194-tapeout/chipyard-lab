# [Optional] MMIO Design

<em><strong>This is a completely optional design exercise. It's digs more deeply into the Chipyard infrastructure and althought an MMIO accelerator is not difficult inconcept, is rather tricky to integrate. There will be limited support for this part in favor of helping the class understand and internalize the previous parts</strong></em>.

Often, an accelerator or peripheral block is connected to the rest of the SoC with a memory-mapped interface over the system bus.
This allows the core and external IO to configure and communicate with the block.

```
generator/
  chipyard/
    src/main/scala/
      example/GCD.scala <--------- If you want to see another example
      unittest/
      config/           <--------- (3) Where we'll test our design
      DigitalTop.scala  <--------- (2) Where we'll connect our deisgn to the rest of the SoC.
      ExampleMMIO.scala <--------- (1) Where we'll design & setup our accelerator.
```

## Setting up & designing our accelerator
Navigate to `$chipyard/generators/chipyard/src/main/scala/ExampleMMIO.scala` where we'll be designing our MMIO Acclerator. Remember, the goal is to desigin an "accelerator" that takes in two 32-bit* values as vectors of 4 8-bit values. The accelerator takes in 32-bit vectors, adds them, and returns the result.

<!--
##### TODO: 32-bit for now; aiming for 64-bit. Turns out not as easy as just change 32 to 64
-->

Most of the logic of the accelerator will go in `VecAddMMIOChiselModule`. This module will be wrapped by the `VecAddModule` which interfaces with the rest of the SoC and determines where our MMIO registers are placed.

**Add the necessary FSM logic into `VecAddMMIOChiselModule`** Notice how `VecAddMMIOChiselModule` has the trait `HasVecAddIO`. The bundle of input.output signals in `HasVecAddIO` are how the accelerator interaces wit the rest of the SoC.

**Inspect `VecAddModule`** There are 3 main sections: setup, hooking up input/outputs, and a regmap. Setup defines the kinds of wire/signals we're working with. We hook up input/output signals as necessary: we feed x and y into the accelerator along with a rest signal and the clock; we expect the result of the addition; we also use a ready/valid interface to signify when the accelerator is busy or avaiable to process fruther instructions. `VecAddTopIO` is used only to see whether the accelerator is busy or not. Then we have the regmap:
<!--
##### TODO: add more detail, expecially about section 1 (regarding DecoupledIO, etc.), maybe some more explaining the IO signals.
-->

* `RegField.r(2, status)` is used to create a 2-bit, read-only register that captures the current value of the status signal when read.
* `RegField.w(params.width, x)` exposes a plain register via MMIO, but makes it write-only.
* `RegField.w(params.width, y)` associates the decoupled interface signal y with a write-only memory-mapped register, causing y.valid to be asserted when the register is written.
* `RegField.r(params.width, vec_add)` “connects” the decoupled handshaking interface vec\_add to a read-only memory-mapped register. When this register is read via MMIO, the ready signal is asserted. This is in turn connected to output_ready on the VecAdd module through the glue logic.

RegField exposes polymorphic `r` and `w` methods that allow read- and write-only memory-mapped registers to be interfaced to hardware in multiple ways.

Since the ready/valid signals of `y` are connected to the `input_ready` and `input_valid` signals of the accelerator module, respectively, this register map and glue logic has the effect of triggering the accelerator algorithm when `y` is written. Therefore, the algorithm is set up by first writing `x` and then performing a triggering write to `y`



## Connecting our design to the rest of the SoC
Once you have these classes, you can construct the final peripheral by extending the `TLRegisterRouter` and passing the proper arguments. The first set of arguments determines where the register router will be placed in the global address map and what information will be put in its device tree entry (`VecAddParams`). The second set of arguments is the IO bundle constructor (`VecAddTopIO`), which we create by extending `TLRegBundle` with our bundle trait. The final set of arguments is the module constructor (`VecAddModule`), which we create by extends `TLRegModule` with our module trait. Notice how we can create an analogous AXI4 version of our peripheral.
<!--
##### TODO: more details about what a TLRegisterRouter is?
-->

`VecAddParams` This is where we define where our MMIO accelerator will be placed. `address` determines the base of the module’s MMIO region (0x2000 in this case). Each TLRouter has default size 4096. Everything `address` to `address` + 4096 is accessibl and only the regions defined in the regmap (as preivously defined) will do anything (reads/writes to other regions will be no-ops).

<!--
##### TODO: explain a bit about the other params.
-->

**Copy paste the following two code blocks into `ExampleMMIO.scala`**
```

class VecAddTL(params: VecAddParams, beatBytes: Int)(implicit p: Parameters)
  extends TLRegisterRouter(
    params.address, "vecadd", Seq("ucbbar,vecadd"),
    beatBytes = beatBytes)(
      new TLRegBundle(params, _) with VecAddTopIO)(
      new TLRegModule(params, _, _) with VecAddModule)

```
```
class VecAddAXI4(params: VecAddParams, beatBytes: Int)(implicit p: Parameters)
  extends AXI4RegisterRouter(
    params.address,
    beatBytes=beatBytes)(
      new AXI4RegBundle(params, _) with VecAddTopIO)(
      new AXI4RegModule(params, _, _) with VecAddModule)

```

Now, we have too hook up everything to the SoC. Rocket Chip accomplishes this using the cake pattern. This basically involves placing code inside traits. In the Rocket Chip cake, there are two kinds of traits: a `LazyModule` trait and a module implementation trait.

The `LazyModule` trait runs setup code that must execute before all the hardware gets elaborated. For a simple memory-mapped peripheral, this just involves connecting the peripheral’s TileLink node to the MMIO crossbar.

**Copy paste the following two code blocks into `ExampleMMIO.scala`**

```
trait CanHavePeripheryVecAdd { this: BaseSubsystem =>
  private val portName = "vecadd"

  // Only build if we are using the TL (nonAXI4) version
  val vecadd = p(VecAddKey) match {
    case Some(params) => {
      if (params.useAXI4) {
        val vecadd = LazyModule(new VecAddAXI4(params, pbus.beatBytes)(p))
        pbus.toSlave(Some(portName)) {
          vecadd.node :=
          AXI4Buffer () :=
          TLToAXI4 () :=
          // toVariableWidthSlave doesn't use holdFirstDeny, which TLToAXI4() needsx
          TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true)
        }
        Some(vecadd)
      } else {
        val vecadd = LazyModule(new VecAddTL(params, pbus.beatBytes)(p))
        pbus.toVariableWidthSlave(Some(portName)) { vecadd.node }
        Some(vecadd)
      }
    }
    case None => None
  }
}
```
```
trait CanHavePeripheryVecAddModuleImp extends LazyModuleImp {
  val outer: CanHavePeripheryVecAdd
  val vecadd_busy = outer.vecadd match {
    case Some(vecadd) => {
      val busy = IO(Output(Bool()))
      busy := vecadd.module.io.vec_add_busy
      Some(busy)
    }
    case None => None
  }
}
```

Note that the `VecAddTL` class we created from the register router is itself a `LazyModule`. Register routers have a TileLink node simply named “node”, which we can hook up to the Rocket Chip bus. This will automatically add address map and device tree entries for the peripheral. Also observe how we have to place additional AXI4 buffers and converters for the AXI4 version of this peripheral.


Now we want to mix our traits into the system as a whole. This code is from `generators/chipyard/src/main/scala/DigitalTop.scala`.

**Copy paste `with chipyard.example.CanHavePeripheryVecAdd` into DigitalTop & `with chipyard.example.CanHavePeripheryVecAddModuleImp` into DigitalTopModule**

Just as we need separate traits for `LazyModule` and module implementation, we need two classes to build the system. The `DigitalTop` class contains the set of traits which parameterize and define the `DigitalTop`. Typically these traits will optionally add IOs or peripherals to the DigitalTop. The `DigitalTop` class includes the pre-elaboration code and also a `lazy val` to produce the module implementation (hence `LazyModule`). The `DigitalTopModule` class is the actual RTL that gets synthesized.

And finally, we create a configuration class in `$chipyard/generators/chipyard/src/main/scala/config/RocketConfigs.scala` that uses the WithVecAdd config fragment defined earlier.

**Copy paste the following**
```
class VecAddTLRocketConfig extends Config(
  new chipyard.example.WithVecAdd(useAXI4=false, useBlackBox=false) ++          // Use VecAdd Chisel, connect Tilelink
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new chipyard.config.AbstractConfig)
```

## Testing Your MMIO

Now we're ready to test our accelerator! We write out test program in `$chipyard/tests/examplemmio.c` Look through the file and make sure you understand the flow of the file.

**Add in a C reference solution for our accelerator**

To generate the binary file of the test, run two following two commands in the terminal

```sh
riscv64-unknown-elf-gcc -std=gnu99 -O2 -fno-common -fno-builtin-printf -Wall -specs=htif_nano.specs -c examplemmio.c -o examplemmio.o
riscv64-unknown-elf-gcc -static -specs=htif_nano.specs examplemmio.o -o examplemmio.riscv
```

Then, navigate to `$chipyard/sims/verilator` and run
```sh
make CONFIG=VecAddTLRocketConfig BINARY=../../tests/examplemmio.riscv run-binary-debug
```
to run the test. If successful, you should see the terminal print whether you passed the test or not. This may take a while.

<!--
##### TODO: maybe something about debugging chisel? making sense of logs?
-->

**Please submit:**
1. The entirety of the code for `VecAddMMIOChiselModule`.
2. Your entire C refenence solution.
3. A screenshot of your test passing.c
4. The google form with questions. These questions are in the sections labeled (TURN IN).
