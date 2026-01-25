# EE194 Lab 1 -- BWRC Compute & Chipyard
You will need your EECS account information for this lab. You should've received an email from EECS IRIS with your @eecs.berkeley.edu account. This is your EECS account/LDAP account. Before you start this lab, follow the instructions in that email to set the password for your account.

## Tour of BWRC Compute
The [Berkeley Wireless Research Center (BWRC)](https://bwrc.eecs.berkeley.edu/) has its own set of machines that we will be using for all future tapeout work. The BWRC compute cluster is a set of machines designed for high intensity compute, and is an active research cluster shared with the lab's graduate student researchers.

Since you will be using an active research compute cluster, please be mindful of your computing practices & follow the guidelines outlined below. 

> [!CAUTION]
> **Not following some of the guidelines can result in BWRC servers freezing & crashing, researchers being unable to log in, lost compute jobs & research work.**

### Compute Cluster Servers
#### Login Servers:
These machines are used as the initial authentication servers for users wanting to log in to the main (more powerful) compute servers. 
```
bwrcrdsl-1.eecs.berkeley.edu (Accepts Remote Desktop via Port 22/SSH, SSH Jump Server)
bwrcrdsl-{2,3,4,5}.eecs.berkeley.edu (SSH Jump Servers)
```
**DO NOT** run any EDA flows (don't run synthesis, or place & route on there), RTL development (VS Code SSH should be connected to the main compute servers - Let us know! We'll show you how), or any Chipyard runs on it.
  * In a previous iteration of 194 Tapeout, someone started Innovus on the login server and it took down bwrcrdsl-1, and no one could remote desktop into the BWRC network for the entire night.

#### Main Compute Servers:
The bulk of your compute work should be done here -- this includes EDA software runs, Chipyard setup & runs, VSCode connections. These have over 120 logical cores/threads & 1TB of DRAM each. However, they need to be logged in from within the BWRC network (from one of the `rdsl` machines).
```
bwrcix-{1,2,3}.eecs.berkeley.edu
```
* VSCode should be connected to these machines. **DO NOT connect VSCode to the login servers - It is too easy to use the integrated terminal in VSCode and launch a large compute job on the wrong server.**
* If you are a BWRC researcher, you will have access to other machines (ex: the AMD machines), however, for consistency, let's do all 194 tapeout work on the IX servers.

### Compute Cluster File System
The BWRC network has ~250TB of networked storage via a [NFS](https://en.wikipedia.org/wiki/Network_File_System) based [Qumulo](https://qumulo.com/) server.

The following locations are available:
* Home Folder: `/bwrcq/users` & `/users/<your username>`
  * **No large files should be stored here.** If you have configuration files for tmux, lightweight software you want to install (bash or zsh utilities, etc) they can be placed here, otherwise keep this area clean.
    * It is generally bad practice to store lots of large files in your home folder on any Linux server, as your home folder is typically on a fast but low capacity boot drive. If you store large files there, it'll take up space and result in other users having very little/no space in their home folders, even for small files.
  * Networked drive, accessible from each machine on the BWRC network, including `rdsl` machines.

* Project space: `/tools/projects`, `/tools/B`, `/tools/C`.
  * All these spaces are networked & accessible across all BWRC machines.
  * `/bwrcq/B` & `/bwrcq/C` are symlinked to `/tools/B` & `/tools/C`, respectively.

* Scratch Space: `/tools/scratch`, `/scratch`
  * `/tools/scratch` is networked, `/scratch` is local to each machine
  * Both scratch spaces claim to be auto-purged after 30 days, but this doesn't seem to be followed in practice... Though, BWRC sysadmins can start enforcing this rule at any point, so no guarantees here.

* Tools & Licensing & PDKs
  * EDA Tools are under `/tools` -- Ex: `/tools/cadence`, `/tools/synopsys`, `/tools/mentor`
    * Note: Siemens Calibre is under `/tools/mentor`
  * EDA Tool Licensing
    * `source /tools/flexlm/flexlm.sh` to include common FLEXLM license variables into your environment.
  * PDKs are located in the `/tools` folder as well:
    * `ts` = TSMC -- Ex: `tstech16c` = `TSMC 16 Compact`
    * `in` = Intel
    * `gf` = GlobalFoundries
    * `sw` = SkyWater

Advice:
* Networked drives are slower than the ones that are local (i.e., `/scratch` is the fastest here). If you are just trying to setup Chipyard or something that has to quickly write a lot of files (as you will see later, Chipyard setup has a lot of file system activity), do this on `/scratch`
* You want to probably setup your main tapeout work on a networked drive so things aren't lost if the sysadmins do a sweep and you forgot to check your work into Git, but things will run a bit slower.
  * If you are good at checking stuff into Git, you can probably live with stuff just being on `/scratch`.

### Git Instance, Remote Desktop
**Git:**
* BWRC runs its own Git server on a locally hosted version of GitLab. You can find it here: https://bwrcrepo.eecs.berkeley.edu/
  * Login information is your EECS account/LDAP
  * You need to be on the Berkeley VPN to access this, even if you are connected to on campus WiFi.

**Remote Desktop:**
* You will need to remote desktop into BWRC machines at times to use a GUI (ex: opening the Innovus GUI to look at Place & Route results).
* The high level overview of this is: Remote desktop into `bwrcrdsl-1`, then from `bwrcrdsl-1`, open a terminal, and SSH with X11 forwarding (`ssh -X`) into a compute server (ex: `bwrcix-1`). 
  * This means when you open up a GUI it'll get X11 forwarded onto `bwrcrdsl`. There is no lag because all servers are wired together in BWRC.
* To set this up: Download [NoMachine](https://www.nomachine.com/), config as follows:
  * Address > Host: `bwrcrdsl-1.eecs.berkeley.edu` -- Only `bwrcrdsl-1` seems to support remote desktop.
  * Configuration > "Use key-based authentication with a key you provide" - Provide the path to your private SSH key.


## Setup your environment
You should know how to do this at this point; so we won't go into too much detail, but here is a checklist of things you might want to do:
* Setup your SSH key for passwordless login to `bwrcrdsl`
  * Note, `ssh-copy-id` will copy the first SSH key you have. If you have multiple SSH keys, you will need to manually copy the public key over under `~/.ssh/authorized_keys` on the remote server.
* Write a SSH config to jump through `bwrcrdsl` to log in to `bwrcix` machines
  * Here is one you can use - add these lines to your `~/.ssh/config` file on your LOCAL MACHINE.
    ```sh
    Host bwrcix-?
      HostName %h.eecs.berkeley.edu
      ProxyJump %r@bwrcrdsl-1.eecs.berkeley.edu
      Port 22
      User <>
      ServerAliveInterval 60
      ForwardX11Trusted yes
    Host bwrcrdsl-?
      HostName %h.eecs.berkeley.edu
      Port 22
      User <>
      ServerAliveInterval 60
      ForwardX11Trusted yes
    ```
  * Then when you connect via VSCode, just type `bwrcix-1` in the window that comes up, it'll automatically use the ssh config above. - To confirm, after connecting, open the integrated terminal and type `hostname` -- make sure this says `bwrcix-1.eecs.berkeley.edu`
* Generate an SSH key on bwrc machines, add it to GitHub & GitLab so you can clone using ssh.
* Configure your local git config -- Example:
  ```
  git config --global user.name "Mona Lisa"
  git config --global user.email "monalisa@example.com"
  ```
* Install an [LSP](https://en.wikipedia.org/wiki/Language_Server_Protocol) - This will help you navigate around the huge amount of Scala that is in Chipyard. We recommend [Scala Metals](https://marketplace.visualstudio.com/items?itemName=scalameta.metals). More options for other editors can be found here: https://scalameta.org/metals/docs/editors/vscode/. 

## Chipyard Overview

![](assets/chipyard-flow.png)

In this lab, we will explore the [Chipyard](https://github.com/ucb-bar/chipyard) framework.

Chipyard is an integrated design, simulation, and implementation framework for open source hardware development developed here at UC Berkeley. 

It is open-sourced online and is based on the Chisel hardware description language. It contains a series of existing IP libraries (known as "generators" in Chipyard world). These generators can generate various blocks that you can configure and combine into an SoC. Chipyard brings together much of the work on hardware design methodology from Berkeley over the last decade as well as useful tools into a single repository that guarantees version compatibility between the projects it submodules (it's sometimes referred to as "the monorepo").

A designer can use Chipyard to build, test, and tapeout (prepare a design for fabrication) a RISC-V-based SoC. This includes RTL development through existing chip IP in Chipyard, cloud FPGA-accelerated simulation with FireSim, and physical design with the Hammer VLSI framework.

Chisel is the primary hardware description language (HDL) used at Berkeley and in Chipyard (This is why we had you do Lab 0 early!). Chipyard supercharges Chisel by providing it a rich set of pre-existing blocks that are written to be compatible to the "agile design" mentatlity -- this means existing generators for "blocks" on a chip that are parameterized, and various different configurations that allow you to combine one block with another. You will be writing an accelerator in Chisel in this lab and coupling it to a Rocket Core (5-stage, in order, RISC-V). This lab aims to familiarize you with the Chipyard framework and give you hands on experience writing a piece of RTL that both use and couples to existing components in Chipyard.

Here are some resources to learn more about Chisel -- See Lab 0:
- [Lab 0](https://github.com/ucb-ee194-tapeout/chisel-labs)
- [Chisel Website](https://www.chisel-lang.org/)
- [Detailed Chisel API Documentation](https://www.chisel-lang.org/api/chisel3/latest/)

Throughout the rest of the course, we will be developing our SoC using Chipyard as the base framework.

This lab provides a brief overview of Chipyard's diverse features, and then guides one through designing, verifying, and incorporating an accelerator into an SoC. This lab will focus more on coupling the accelerator to the SoC via the [RoCC](https://chipyard.readthedocs.io/en/latest/Customization/RoCC-Accelerators.html) protocol, but will also briefly touch on coupling via [MMIO](https://chipyard.readthedocs.io/en/latest/Customization/RoCC-or-MMIO.html). 

## Setup Chipyard
Go to your folder of choice (`/scratch/` or `/tools/C` - See above) -- We'll use `/tools/C/$USER` as an example. Where `$USER` is your EECS IRIS account username. **REPLACE ALL INSTANCES OF `/tools/C` WITH `/scratch` IF YOU DECIDE TO USE SCRATCH.**

Please create your user folder if it doesn't exist, by running
```sh
mkdir /tools/C/$USER
```

and cd into this folder.

```sh
cd /tools/C/$USER
```

If you run into any issues, please contact course staff. <b>DO NOT</b> work out of the home directory `~`.


**IMPORTANT:**
Some of these next commands will take >30 minutes to run. Before running these commands, please use [tmux](https://github.com/tmux/tmux), this allows you to <b>SAVE YOUR TERMINAL SESSIONS</b>: the basic commands to know for tmux are as follows:
```sh
tmux                        # creates a new session
tmux ls                     # list sessions
tmux attach -t <session #>  # attach to a particular session, listed by tmux ls
control+b d                 # detach from a session
```


1) Run
```sh
source /tools/C/ee194-sp26/bwrc-env.sh # this sources /tools/flexlm/flexlm.sh for you 
```

This script is responsible for setting up the tools and environment used in this lab (and more generally by the course). You should vim into the file to see what it does.

**You will need to source this script in every new terminal & at the start of every work session, or put this in your bashrc or zshrc**

2) Clone the lab chipyard repo: [git@bwrcrepo.eecs.berkeley.edu:ee194-290c-sp26/sp26-staff-only/chipyard-ee194.git](git@bwrcrepo.eecs.berkeley.edu:ee194-290c-sp26/sp26-staff-only/chipyard-ee194.git).
```sh
git clone git@bwrcrepo.eecs.berkeley.edu:ee194-290c-sp26/sp26-staff-only/chipyard-ee194.git ee194-lab1
```

3) Run
```sh
cd ee194-lab1
```

4) Run
```sh
export chipyard=/tools/C/$USER/ee194-lab1
```
to set the repo path as an [environment variable](https://www.geeksforgeeks.org/environment-variables-in-linux-unix/). We will be referring to the repo path as `$chipyard` from now on. You might also see `${CY}` used at times as well. They mean the same thing.

5) Run

```sh
chgrp tstech16c .
chmod 2750 .
```

This is because since we will be working with TSMC proprietary information in our repository, we must limit the access of this directory to only those within the `tstech16c` Unix group. You should do this for any folder containing TSMC proprietary information.


6) To use Conda w/ Miniforge, like the [**Official Chipyard
Setup**](https://chipyard.readthedocs.io/en/stable/Chipyard-Basics/Initial-Repo-Setup.html), run the following commands.

```sh
wget "https://github.com/conda-forge/miniforge/releases/latest/download/Miniforge3-$(uname)-$(uname -m).sh"
bash Miniforge3-$(uname)-$(uname -m).sh -b
```

Then, in the same terminal, run this command to activate the conda environment. (Note that this will automatically run the next time you start a new session)
```sh
source ~/.bashrc
```

After conda installation, you would nominally see `(base)` at the right side of your command prompt.
If you're not seeing that and nothing shows up after `which conda`, try
```sh
source ~/miniforge3/bin/activate
```
and see if your terminal session has the conda-related commands and paths imported.

Now run the following commands to complete the Chipyard setup.
```sh
conda config --set channel_priority true
conda install -n base conda-libmamba-solver
conda config --set solver libmamba
conda install -n base conda-lock==1.4.0
conda activate base
```

<!-- 6) Run
```
conda activate /tools/C/ee290-sp25/chipyard/.conda-env/
```

This command, in Chipyard, uses the Conda package manager to help manage system dependencies. Conda creates a virtual environment that holds system dependencies like `make`, `gcc`, etc. We've also installed a pre-built RISC-V toolchain into it. We want to ensure that everyone in the class is using the same version of everything, so everyone will be using the same conda environment by activating the environment specified above. <b>You will need to do this in every new terminal & at the start of every work session.</b> -->

Finally run

```sh
./build-setup.sh riscv-tools -s 6 -s 7 -s 8 -s 9
```
To setup and build chipyard.

<!-- The `init-subodules-no-riscv-tools.sh` script will initialize and checkout all of the necessary `git submodules`. This will also validate that you are on a tagged branch, otherwise it will prompt for confirmation. When updating Chipyard to a new version, you will also want to rerun this script to update the submodules. Using git directly will try to initialize all submodules; this is not recommended unless you expressly desire this behavior.

`git submodules` allows you to keep other Git repositories as subdirectories of another Git repository. For example, the above script initiates the `rocket-chip` submodule which is it's own Git repository that you can look at <a href="https://github.com/chipsalliance/rocket-chip/tree/44b0b8249279d25bd75ea693b725d9ff1b96e2ab">here</a>. If you look at the `.gitmodules` file at `$chipyard`, you can see
```
[submodule "rocket-chip"]
	path = generators/rocket-chip
	url = https://github.com/chipsalliance/rocket-chip.git
```
which defines this behavior. Read more about `git submodules` [here](https://git-scm.com/book/en/v2/Git-Tools-Submodules). -->

7) In the top level Chipyard folder, Run
```sh
source ./env.sh
```

An `env.sh` file should exist in the top-level repository (`$chipyard`). This file sets up necessary environment variables such as `PATH` for the current Chipyard repository. This is required by future Chipyard steps such as the `make` system to function correctly.

Over the course of the semester, we will find ourselves working with different Chipyards, such as one for this lab, and one for the SoCs we build this semester.

<!--- An `env.sh` file should exist in the top-level repository (`$chipyard`). This file sets up necessary environment variables such as needed for future Chipyard steps (needed for the `make` system to work properly). Once the script is run, the `PATH`, `RISCV`, and `LD_LIBRARY_PATH` environment variables will be set properly for the toolchain requested. -->

You should source the `env.sh` file in the Chipyard repository you wish to work in <!--- in your [`.bashrc`](https://www.digitalocean.com/community/tutorials/bashrc-file-in-linux) or equivalent environment setup file to get the proper variables, or directly include it in your current environment --> by **running the above command every time you open a new terminal or start a new work session**.

This concludes the Chipyard setup.

**Below, we list the commands that should be run at every new terminal session.**
```sh
export chipyard=/tools/C/$USER/ee194-lab1
cd $chipyard
source ~/miniforge3/bin/activate
source ./env.sh
source /tools/C/ee194-sp26/bwrc-env.sh 
```

You can write these into a shell script or bash alias that you call upon first login to source everything you need in 1 command. Historically we've seen students sometimes run into issues logging in over NoMachine when including these commands in their `.bashrc`. Hence we recommend setting up a shell script or bash alias you run manually instead of automatically running these during log in.


## Chipyard Repo Files & Directories Overview

<!-- > <b>You will mostly be working out of the `generators/` (for designs), `sims/vcs/` (for simulations)* and `vlsi/` (for physical design) directories.</b>
However, we will still give a general repo tour to get you familiar with Chipyard as a whole.


###### *VCS is a propietory simulation tool provided by Synopsys while Verilator is an open-source tool. There are some subtle differences form the user perspective, but VCS is usually faster so we'll be using that throuhgout the course. Everthing done with VCS can easily also be done in Verilator (the subdirectory structure is the same as well). -->

```
 $chipyard == ${CY}/
  generators/ <------- library of Chisel generators
    chipyard/ <------------- Special Chipyard generator to integrate designs
    sha3/ <------------- (example) sha3 accelerator: ucb.bar/sha3  
    ... <------------- many other generators: BOOM, Saturn, Rocket, etc!
  sims/ <------------- utilities for RTL simulation of SoCs
    vcs/ <------------- Synopsys RTL simulator
    verilator/ <------------- Open-source RTL simulator
    firesim/ <------------- Berkeley's FPGA accelerated & optimized RTL simulator
    xcelium/ <------------- Cadence RTL simulator
  fpga/ <------------- scripts + shells for creating FPGA prototypes (≠ FireSim!)
  software/ <------------- Bringup infra: Baremetal IDE, Zephyr RTOS, 
                           Example workloads, FireSim workload builder: FireMarshal
  vlsi/ <------------- VLSI using Hammer flow manager
  toolchains/ <------- RISC-V compiler toolchains, C++ based ISA sims, Proxy Kernel
  tools/ <------------- Scala-based tools usable in Scala/Chisel designs
  tests/ <------------- Some provided C tests & headers for custom C tests
  scripts/ <------------- Chipyard build system scripts
  conda-reqs/ <------------- Folder for Chipyard dependencies, managed by Conda
  docs/ <------------- Folder for all docs on https://chipyard.readthedocs.io
  build.sbt <------------- Top level build file for all Scala code in Chipyard
  common.mk <------------- Top level Make file for all Chipyard Make commands
```

Chipyard documentation can be found
[here](https://chipyard.readthedocs.io/en/latest/).

You may have noticed while initializing your Chipyard repo that there are many submodules. Chipyard is built to allow the designer to generate complex configurations from different projects including the in-order Rocket Chip core, the out-of-order BOOM core, the systolic array Gemmini, and many other components needed to build a chip.

You can find most of these in the `$chipyard/generators/` directory.
All of these modules are built as generators (a core driving point of using Chisel), which means that each piece is parameterized and can be fit together with some of the functionality in Rocket Chip (check out the [TileLink and Diplomacy references](https://chipyard.readthedocs.io/en/stable/TileLink-Diplomacy-Reference/index.html) in the Chipyard documentation).

## Chipyard SoC Configs & Architectures

<table border-"0">
  <tr>
    <td><img src="assets/tutorial/rtl_gen_layer.png" width=700 /></td>
    <td><img src="assets/tutorial/chipyard.jpg" /></td>
  </tr>
</table>



<table border="0">
 <tr>
    <td><img style="float: left;" src="assets/tutorial/tile.jpg" width="200"></td>
    <td>
      <h2>Tiles</h2>
      <ul>
        <li> A tile is the basic unit of replication of a core and its associated hardware
        <li> Each tile contains a RISC-V core and can contain additional hardware such as private caches, page table walker, TileBus (specified using configs)
        <li> Several varieties of cores (<a href="https://chipyard.readthedocs.io/en/stable/Generators/Rocket.html">Rocket</a>, <a href="https://chipyard.readthedocs.io/en/stable/Generators/BOOM.html">BOOM</a>, <a href="https://chipyard.readthedocs.io/en/stable/Generators/Sodor.html">Sodor</a>, <a href="https://chipyard.readthedocs.io/en/stable/Generators/CVA6.html">CVA-6 (Ariane)</a>, <a href="https://chipyard.readthedocs.io/en/stable/Generators/Ibex.html">Ibex</a> supported)
        <li> Interface supports integrating your own RISC-V core implementation
      </ul>
    </td>
  </tr>

  <tr>
    <td><img style="float: left;" src="assets/tutorial/rocc.jpg" width="200"></td>
    <td>
      <h2>RoCC Accelerators</h2>
      <ul>
        <li> Tightly-coupled accelerator interface
        <li> Attach custom accelerators to Rocket or BOOM cores
        <li> Example: <a href="https://github.com/ucb-bar/gemmini/tree/c47cb7f3eb5c18390f176f3a53c43c8546d487d2">GEMMINI accelerator</a>
      </ul>
    </td>
  </tr>


  <tr>
    <td><img style="float: left;" src="assets/tutorial/mmio.jpg" width="200"></td>
    <td>
      <h2>MMIO Accelerators</h2>
      <ul>
        <li> Controlled by memory-mapped IO registers
        <li> Support DMA to memory system
        <li> Examples: <a href="http://nvdla.org/">Nvidia NVDLA accelerator</a> & <a href="https://chipyard.readthedocs.io/en/stable/Generators/fft.html">FFT accelerator generator </a>
      </ul>
    </td>
  </tr>

  <tr>
    <td>
      <table border="0">
        <tr>
        </tr>
        <tr>
          <td><img style="float: left;" src="assets/tutorial/tilelink.jpg" width="200"></td>
        </tr>
        <tr>
          <td><img style="float: left;" src="assets/tutorial/noc.jpg" width="200"></td>
        </tr>
      </table>
    </td>
    <td>
      <table border="0">
        <tr>
          <td><h2>Chip Interconnect</h2></td>
        </tr>
        <tr>
          <td>
            <h3>TileLink Standard</h3>
            <ul>
              <li> TileLink is an open-source chip-scale interconnect standard (i.e., a protocol defining the communication interface between different modules on a chip)
              <li> Comparable to industry-standard protocols such as AXI/ACE
              <li> Supports multi-core, accelerators, peripherals, DMA, etc.
            </ul>
            <h3>Interconnect IP in Chipyard</h3>
            <ul>
              <li> Library of TileLink RTL generators provided in RocketChip
              <li> RTL generators for crossbar-based buses
              <li> Width-adapters, clock-crossings, etc.
              <li> Adapters to AXI4, APB
            </ul>
          </td>
        </tr>
        <tr>
          <td>
            <h3>Constellation</h3>
            <ul>
              <li> A parameterized Chisel generator for SoC interconnects
              <li> Protocol-independent transport layer
              <li> Supports TileLink, AXI-4
              <li> Highly parameterized
              <li> Deadlock-freedom
              <li> Virtual-channel wormhole-routing
            </ul>
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <tr>
    <tr>
      <td><img style="float: left;" src="assets/tutorial/shared_mem.jpg" width="200"></td>
      <td>
        <h2>Shared Memory</h2>
        <ul>
          <li> Open-source L2 cache that communicates over TileLink (developed by SiFive, iykyk)
          <li> Directory-based coherence with MOESI-like protocol
          <li> Configurable capacity/banking
          <li> Support broadcast-based coherence in no-L2 systems
          <li> Support incoherent memory systems
        </ul>
        <h2>DRAM</h2>
        <ul>
          <li> AXI-4 DRAM interface to external memory controller
          <li> Interfaces to DRAM simulators such as DRAMSim/FASED
        </ul>
      </td>
    </tr>
  </tr>

  <tr>
    <td><img style="float: left;" src="assets/tutorial/peripherals.jpg" width="200"></td>
    <td>
      <h2>Peripherals and IO</h2>
      <ul>
        <li>  <a href="https://docs.google.com/document/d/13rCqMM0qARjcLTrkwqlTzNClU-cxjUnZE0jHnIoe4UU/edit?usp=sharing">Chipyard Peripheral User Manual </a>  put together by Yufeng Chi who took the Sp22 iteration of this class. This document is a living document, so feel to add comments on sections that you don't understand/woud like to see added.
        <li> Open-source RocketChip + SiFive blocks:
        <ul>
          <li> Interrupt controllers
          <li> JTAG, Debug module, BootROM
          <li> UART, GPIOs, SPI, I2C, PWM, etc.
        </ul>
        <li> TestChipIP: useful IP for test chips
        <ul>
          <li> Clock-management devices
          <li> SerDes
          <li> Scratchpads
        </ul>
       <li>Documentations of the peripheral devices can be found <a href="https://drive.google.com/file/d/1aDYtmHgG30Gy591TaNlya2rcc54nn9gZ/view?usp=sharing">here</a></li>
      </ul>
    </td>
  </tr>

</table>

<table border-"0">
  <tr>
    <td><img src="assets/tutorial/config_gen_layer.png" width=1000 /></td>
    <td><img alt="How Configs Work" src="assets/tutorial/02_chipyard_basics.gif" width=660></td>
  </tr>
</table>




### Exercise: Interpreting a Config
**For all of the exercises in this lab, please submit your answers / follow along on [this](https://docs.google.com/forms/d/e/1FAIpQLSffo6ygIZy4s1R3iwE7iIs5wsAZyIvH5NQtlochZiHMh9rCFg/viewform?usp=publish-editor) Google Form.**

Chipyard Configs describe what goes into our final system and what paramters our designs are elaborated with. You can find the configs in `$chipyard/generators/chipyard/src/main/scala/config`.

Look at the configs located in `$chipyard/generators/chipyard/src/main/scala/config/RocketConfigs.scala`, specifically `RocketConfig`

```scala
class RocketConfig extends Config(
  new freechips.rocketchip.rocket.WithNHugeCores(1) ++           // single rocket-core
  new chipyard.config.AbstractConfig)                            // builds one on top of another, so the single rocket-core is built on top of the AbstractConfig
```

<table border-"0">
  <tr>
    <td>
    RocketConfig is part of the "Digital System configuration" depicted below. It is built on top of the AbstractConfig which contains the config fragments (each line like <code>freechips.rocketchip.rocket.WithNHugeCores(1)</code> that adds something to the overall system is called a config fragment) for IO Binders and Harness Binders (depicted below).
    </td>
    <td><img src="assets/tutorial/io_high_level.jpg" width = 1700/></td>
  </tr>
</table>



<table border-"0">
  <tr>
    <td><img src="assets/tutorial/io_harness.jpg" /></td>
    <td><img src="assets/tutorial/io_harness_map.jpg"/></td>
  </tr>
</table>

<table>
  <tr>
    <th>Question</th>
    <th>Answer</th>
    <th>How we found the answer?</th>
  </tr>
  <tr>
    <td>Is UART enabled? If so, which config fragments enabled it?</td>
    <td>Yes; <code>chipyard.config.WithUART</code>,  <code>chipyard.iobinders.WithUARTIOCells</code>,  <code>chipyard.harness.WithUARTAdapter</code>.</td>
    <td>We grep for <code>AbstractConfig</code> in <code> $chipyard/generators/chipyard/src/main/scala/</code> and find <code>AbstractConfig</code> at <code>$chipyard/generators/chipyard/src/main/scala/config/AbstractConfig.scala</code>. We search for <code>UART</code> and find the corresponding config fragments.</td>
  </tr>
  <tr>
    <td>How many bytes are in a block for the L1 DCache? How many sets are in the L1 DCache? Ways?</td>
    <td>64 Block Bytes, 64 Sets, 4 Ways</td>
    <td>We don't see anything about L1 DCaches in <code>AbstractConfig</code>, so we grep for <code>WithNBigCores</code> at <code>$chipyard/generators/rocket-chip/src/main/scala/</code>. We find it in <code>$chipyard/generators/rocket-chip/src/main/scala/subsystem/Configs.scala</code> We see that the fragment instantiates a dcache with <code>DCacheParams</code> We notice it passes in <code>CacheBlockBytes</code> to blockBytes. So, we grep for <code>CacheBlockBytes</code> in <code>$chipyard/generators/rocket-chip/src/main/scala/</code> and see <pre><code>src/main/scala/subsystem/BankedL2Params.scala:case object CacheBlockBytes extends Field[Int](64)</code></pre> Then, we grep for <code>DCacheParams</code> and find it in<code>$chipyard/generators/rocket-chip/src/main/scala/rocket/HellaCache.scala</code> where we find the <code>nSets</code> and <code>nWays</code> fields</td>
  </tr>
  <tr>
    <td>Is there an L2 used in this config? What size?</td>
    <td>Yes. 1 bank, 8 ways, 512Kb.</td>
    <td>We once again start looking at <code>RocketConfig</code> which leads us to <code>AbstractConfig</code>. Looking at the comments of the various config fragments we see the comment <code> /** use Sifive LLC cache as root of coherence */</code> next to <code> new freechips.rocketchip.subsystem.WithInclusiveCache ++</code> (You can read more about SiFive <a href="https://www.sifive.com/">here</a>). We could have grepped in the generators directory for <code>WithInclusiveCache</code> or noticed that a <code>rocket-chip-inclusive-cache</code> submodule existed under <code>$chipyard/generators</code>. Navigating through it we eventually find the <code>WithInclusiveCache</code> class at <code>$chipyard/generators/rocket-chip-inclusive-cache/design/craft/inclusivecache/src/Configs.scala</code>.</td>

  </tr>
</table>

Inspect `MysteryRocketConfig` & answer the following questions. You should be able to find the answers or clues to the answers by grepping (`grep -nr "<name>"`) in `$chipyard/generators/chipyard/src/main/scala/` or `$chipyard/generators/rocket-chip/src/main/scala/` or using your LSP to follow each part of the config to find where they are defined. Feel free to also Google/search the Chipyard documentation.
<!-- 
**1. How many bytes are in a block for the L1 DCache? How many sets are in the L1 DCache? Ways?** -->

**1. How many bytes are in a block for the L1 ICache? How many sets are in the L1 ICache? Ways?**

**2. What type of L1 Data Memory does this configuration have?**

**3. Does this configuration support custom boot? If yes, what is the custom boot address?**

**4. What accelerator does this config contain? Is this accelerator connected through RoCC or MMIO?**

**5. Does this config include a FPU (floating point unit)?**

**6. Does this config include a multiply-divide pipeline?**

## Exercise: Compiling a Config 

Let's run some commands! **MAKE SURE YOU RUN THESE ON THE BWRCIX-\* MACHINES**.

We'll be running the `CONFIG=RocketConfig` config (the `-j32` executes the run with more threads). This compiles the Chipyard configuration we saw above, converting Rocket Core's Chisel RTL into Verilog (Remember when we talked about how this happens in lab 0 part 0? Hint: FIRRTL)... Run:
```sh
cd $chipyard/sims/vcs
make -j32 CONFIG=RocketConfig
```
> *Notes: [error] `Picked up JAVA_TOOL_OPTIONS: -Xmx8G -Xss8M -Djava.io.tmpdir=` is not a real error. You can safely ignore it.*

After the run is done, check the `$chipyard/sims/vcs/generated-src/` folder. Find the directory of the config (`chipyard.harness.TestHarness.<CONFIG>`) that you ran and you should see the following files under `/gen-collateral` in that directory:
- `ChipTop.sv`: Synthesizable Verilog source
- `TestHarness.sv`: TestHarness that instantiates ChipTop so it can be RTL simulated
- `XXX.dts`: device tree string
- `../XXX.memmap.json`: memory map (one directory above `gen-collateral`)

Answer the following question:

**1. Try to find the top-level SystemVerilog/Verilog modules that correspond to the ICache/DCache. What are they called? For ICache, name one module that is instantiated in the SystemVerilog file that contains the *implementation (not instantiation)* for the ICache. *Hint: what modules look like they represent memories?***

## RTL Simulation of SoCs in Chipyard

A simple RISC-V test can be found under `$RISCV/riscv64-unknown-elf/share/riscv-tests/isa/`and can be run in `$chipyard/sims/vcs` as:
```sh
make run-binary CONFIG=RocketConfig BINARY=$RISCV/riscv64-unknown-elf/share/riscv-tests/isa/rv64ui-p-simple
```

This runs the RISC-V binary `rv64ui-p-simple` on the RTL defined by the Chipyard Config `RocketConfig`.

**1. What are the last 10 lines of the `.out` file generated by the assembly test you ran? It should include the *** PASSED *** flag.**

**2. How many cycles did the simulation take to complete?**

**3. What is the hexadecimal representation of the last instruction run by the CPU?**

In summary, when we run something like:
```sh
make run-binary CONFIG=RocketConfig BINARY=$RISCV/riscv64-unknown-elf/share/riscv-tests/isa/rv64ui-p-simple
```
The first part of the command (`CONFIG=RocketConfig`) will elaborate the design and create SystemVerilog/Verilog.

This is done by converting the Chisel code, embedded in Scala, into a FIRRTL intermediate representation which is then run through the FIRRTL compiler to generate Verilog (for more details, see lab 0).

Next, it will run VCS (hence the `sims/vcs` folder) to build a simulator out of the generated Verilog that can run RISC-V binaries.

The second part of the command (`BINARY=...`) will run the test specified by `BINARY` and output results as an `.out` file.

This file will be emitted to the `$chipyard/sims/vcs/output/` directory.

Many Chipyard/Chisel-based designs look like a Rocket core connected to some kind of "accelerator" (e.g. a DSP block like an FFT module).

When building something like this, you would typically build your "accelerator" generator in Chisel. Then you can choose to unit test using Chisel Test or run integration tests. 

Chisel Test is the kind of test that we asked you to write in Lab 0: See [Chisel Testing](#chisel-testing) section on how to setup & run Chisel Tests.

Integration tests (eg. a baremetal C program) are tests that can be simulated with your Rocket Chip and "accelerator" block together to test end-to-end system functionality. See [Baremetal Functional Testing](#baremetal-functional-testing).


### Chisel Testing
To compile the design and run our tests, we use the Scala Build Tool (sbt). `$chipyard/build.sbt` (in the root Chipyard directory) contains project settings, dependencies, and sub-project settings. 

#### Setting up Chisel Test

Tests go in `<your generator project>/src/test/scala/TestXYZ.scala`.

Here is the boilerplate to get you started.

```scala
package <YOUR PROJECT NAME>

import chisel3._
import chisel3.util._
import chiseltest._
import org.chipsalliance.cde.config._
import freechips.rocketchip.tile._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import org.scalatest.flatspec.AnyFlatSpec

class XYZTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "SOME HARDWARE"

  it should "DO SOMETHING" in {
    test(new <YOUR MODULE CLASS NAME>()(Parameters.empty)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteFstAnnotation)) { c =>
      // NORMAL CHISEL TEST SYNTAX BELOW 

      /* expect ready */
      c.io.cmd.ready.expect(true.B)
    
      /* send input */
      c.io.cmd.bits.rs1.poke("h_07_06_05_04_03_02_01_00".U)
      c.io.cmd.bits.rs2.poke("h_08_07_06_05_04_03_02_01".U)
      c.io.cmd.valid.poke(/* YOUR CODE HERE */)

      /* step the clock by 1 cycle */
      /* YOUR CODE HERE */

      /* expect valid */
      c.io.resp.valid.expect(true.B)
      c.io.resp.ready.poke(true.B)
      c.io.resp.bits.data.expect(/* YOUR CODE HERE */)
    
      println("Observed output value :" + c.io.resp.bits.data.peek().litValue)
    
      c.clock.step(1)

    }
  }
}

```

In a new terminal window inside **the root Chipyard directory**, run:
```sh
sbt
```

Give it a minute or so to launch the sbt console and load all settings.

In the sbt console, set the current project by running:
```
sbt:chipyardRoot> project <YOUR PROJECT NAME>
```

To compile the design, run `compile` in the sbt console, as follows:
```
sbt:<YOUR PROJECT>> compile
```
This might take a while as it compiles all dependencies of the project.

To run all tests, run `test` in the sbt console, as follows:
```
sbt:<YOUR PROJECT>> test
```

Exit the sbt console with:
```
sbt:<YOUR PROJECT>> exit
```

(You can use `testOnly <test names>` to run specific ones.) Test outputs will be visible in the console. You can find waveforms and test files in `$chipyard/test_run_dir/<test_name>`.

Use `gtkwave` to inspect the waveform at `$chipyard/test_run_dir/Basic_Testcase/TESTNAME.fst`.

### Baremetal Functional Testing

Let's look at any C test in `$chipyard/generators/packbits-acc/baremetal-test`.

You will see the inclusion of the `rocc.h` header. It contains definitions for different kinds of RoCC instructions and the custom opcodes. The header contains helper C macros for assembly instructions that allow us to sent instructions to the RoCC accelerator.

Inline assembly instructions in C are invoked with the `asm volatile` command. Before the first instruction, and after each RoCC instruction, the fence command is invoked. This ensures that all previous memory accesses will complete before executing subsequent instructions, and is required to avoid mishaps as the Rocket core and coprocessor pass data back and forth through the shared data cache. (The processor uses the “busy” bit from your accelerator to know when to clear the fence.) A fence command is not strictly required after each custom instruction, but it must stand between any use of shared data by the two subsystems.

While one can compute results for each test case and test for equality against the accelerator's results, such a strategy is not reliable nor scalable as tests become complex - such as when using random inputs or writing multiple tests. 

Thus, there lies significant value in writing a functional model that performs the same task as the accelerator, but in software. Of course, care must be taken in writing a correct functional model that adheres to the spec.

**Inspect `$chipyard/tests/rocc.h`**.

Answer the following question:

**1. What assembly instruction does ROCC_INSTRUCTION_DSS stand for? You don't need to include the entire instruction, just the first 5 characters will be sufficient. What argument does ROCC_INSTRUCTION_DSS accept -- When would you use it?**

Next, we compile our test by running the following in the `$chipyard/generators/packbits-acc/baremetal_test` directory:
```sh
riscv64-unknown-elf-gcc -fno-common -fno-builtin-printf -specs=htif_nano.specs -c TestPackBitsDecompHello.c
riscv64-unknown-elf-gcc -static -specs=htif_nano.specs TestPackBitsDecompHello.o -o TestPackBitsDecompHello.riscv
```
> Note: In the Baremetal Tests folder for the accelerator you will be implementing, we have provided a Makefile to automatically compile all tests.

Here, we're using a version of gcc with the target architecture set to riscv (without an OS underneath). This comes as part of the riscv toolchain. Since we want a self-contained binary, we compile it statically.

Now, let's disassemble the executable `functionalTest` by running:
```sh
riscv64-unknown-elf-objdump -d TestPackBitsDecompHello.riscv | less
```

Inspect the output. Answer the following question:

**2. What is the address of the `ROCC_INSTRUCTION_SS`?**
Looking through `<main>` and looking for `opcode0` should be helpful.

We can run this baremetal test with the following command. These tests will run after you implement your accelerator.

When running, make sure you are in `$chipyard/sims/vcs`, then run:
```sh
make -j32 CONFIG=PackBitsConfig BINARY=../../generators/packbits-acc/baremetal_test/TestPackBitsDecompHello.riscv run-binary-debug SIM_FLAGS="-debug_accss+all"
```

It might take a few minutes to build and compile the test harness, and run the simulation.

Inside, `$chipyard/sims/vcs`, for each config,
- `generated-src` contains the test harness
- `output` contains output files (log/output/waveform) for each config.

**Waveforms:**
Use `verdi -ssf <fsdb file>`. Synopsys has transitioned to a new waveform viewer called Verdi that is much more capable than DVE. Verdi uses an open file format called *fsdb* (Fast Signal Database), and hence VCS has been set up to output simulation waveforms in fsdb.

In the bottom pane of your Verdi window, navigate to `Signal > Get Signals...`. Follow the module hierarchy to the correct module.
```
TestDriver
  .testHarness
    .chiptop
      .system
        .tile_prci_domain
          .element_reset_domain_rockettile
            .packbitsacc
```


Chipyard provides the infrastructure to help you further simulate, verify & implement your design as depicted below:

<table border-"0">
  <tr>
    <td>

- <b>SW RTL Simulation:</b> RTL-level simulation with VCS or Verilator or Xcelium. If you design anything with Chipyard, you should be running SW RTL simulation to test.
- <b>Hammer VLSI flow:</b> Tapeout a custom config in some process technology.
- <b>FPGA prototyping:</b> Fast, non-deterministic prototypes (we won't be doing this in this class).
- <b>FireSim:</b> Fast, accurate FPGA-accelerated simulations (we won't be using this in this class, but if you're curious about FireSim, check out its documentation [here](https://fires.im/) and feel free to reach out to a TA to learn more).

    <td><img src="assets/tutorial/high_sim.jpg" width = 1500/></td>
  </tr>
</table>

## In summary...

<table border-"0">
  <tr>
    <td>

- <b>Configs</b>: Describe parameterization of a multi-generator SoC.
- <b>Generators</b>: Flexible, reusable library of open-source Chisel generators (and Verilog too).
- <b>IOBinders/HarnessBinders</b>: Enable configuring IO strategy and Harness features.
- <b>FIRRTL Passes</b>: Structured mechanism for supporting multiple flows.
- <b>Target flows</b>: Different use-cases for different types of users.</td>
    <td><img src="assets/tutorial/chipyard_summary.jpg" /></td>
  </tr>
</table>



# Designing a Custom Accelerator
The idea here is to learn how to incorporate a custom RoCC accelerator in an SoC by writing an accelerator generator and effectively utilizing the simplicity and extensibility of Chipyard. This accelerator will involve a decent amount of Chisel. We recognize that not everyone taking the course will be interested in writing RTL, but even if you do not plan to be on the RTL team, being able to read RTL in Chisel that is being used in Chipyard will be critical.
  * Ex: The verification team will likely need to read chip's RTL to figure out how to test effectively, what the interfaces, etc. look like... When something breaks, determine which RTL team to go talk to.
  * The PD team will need to understand chip level IO, which modules talk to which other module in order to determine floorplanning, which modules have RTL elements that can result in physical design inefficiencies (large queues resulting in register banks, large shifters, etc). 

## RoCC Accelerator Interface

- RoCC stands for Rocket Custom Coprocessor.
- A block using the RoCC interface sits on a Rocket Tile.
- Such a block uses custom non-standard instructions reserved in the RISC-V ISA encoding space.
- It can communicate using a ready-valid interface with the following:
  - A core on the Rocket Tile, such as BOOM or Rocket Chip (yes, it's an overloaded name :))
  - L1 D$
  - Page Table Walker (available by default on a Rocket Tile)
  - SystemBus, which can be used to communicate with the outer memory system, for instance
- Additional details about the format of the RoCC instructions and how software can use these instructions to interact with the custom accelerator can be found in the PackBits RLE accelerator Hardware Architectural Specification.

<p align="center">
  <img alt="RoCC Interface" src="assets/tutorial/RoCC Interface.png" width=760>
</p>

For more on RoCC, we encourage you to refer to Sections [6.5](https://chipyard.readthedocs.io/en/latest/Customization/RoCC-or-MMIO.html) and [6.6](https://chipyard.readthedocs.io/en/latest/Customization/RoCC-Accelerators.html) of the Chipyard docs & related examples:
* [mempress](https://ucb.bar/mempress)
* [compressor-acc](https://ucb.bar/compress-acc)
* [protoacc](https://ucb.bar/protoacc)
<!-- 2. Bespoke Silicon Group's [RoCC Doc V2](https://docs.google.com/document/d/1CH2ep4YcL_ojsa3BVHEW-uwcKh1FlFTjH_kg5v8bxVw/edit) -->


## Accelerator Specification & Task List

The TL;DR of the implementation is: you will be implementing a [PackBit Run-Length Encoding](https://en.wikipedia.org/wiki/PackBits) decompressor RoCC accelerator.

You will need to implement parts or all of the following:

a) The Command Router module, which takes parses & extracts incoming RoCC instructions.
  * It will extract the address to read/load data from & the number of bytes to read from an incoming RoCC instruction, then enqueue them into a Queue, which will be consumed by a provided L2$ DMA reader engine.
  * It will extract the address to write decompressed results to from an incoming RoCC instruction, then enqueue them into a Queue, which will be consumed by a provided L2$ DMA writer engine.
  * See the baremetal C tests for what fields of the RoCC instructions contain what information. The first RoCC instruction seen in C tests contains the address to load from & amount of bytes to load. The second RoCC instruction in the C tests contain a destination memory address for results to be written to.
  * The file to edit is `$chipyard/generators/packbits-acc/src/main/scala/PackBitsCommandRouter.scala`.

b) The PackBits Decompress module, which performs the PackBits RLE decompression.
  * See the Wikipedia article above for how PackBit RLE compression works.
  * The module will dequeue from a Queue that contains the read data from the L2$ DMA reader engine. Each element of the Queue (representing each beat of the DMA reader engine) is 256-bits. A naive implementation of PackBit RLE decompression operates on each byte one at a time.
    * You will need to figure out how to buffer the 256-bit input and process byte by byte.
  * Decompressed data (in the form of a byte stream) will be enqueued into a Queue for the L2$ DMA writer engine to consume & write back. Each element of this Queue (each DMA writer engine beat) is 256-bits.
    * You will need to figure out how to buffer the processed bytes until a 256-bit series of processed bytes can be assembled and enqueued for the DMA writer.
  * Your implementation should go in the file `$chipyard/generators/packbits-acc/src/main/scala/PackBitsDecompressModule.scala`
  * Software functional models are available [here in Python](https://github.com/psd-tools/packbits/blob/master/src/packbits.py#L4-L26), [here in JavaScript](https://en.wikipedia.org/wiki/PackBits).

c) Integrating your accelerator into the Chipyard build system.
  * **You should do this first so you can run the Baremetal tests and get a waveform for debugging.**
  * This involves editing the top level `build.sbt` file to include the project at `$chipyard/generators/packbits-acc`. The `build.sbt` file for the `packbits-acc` project is provided and lives at `$chipyard/generators/packbits-acc/build.sbt`.
  * Once you have integrated the project into Chipyard's top level `build.sbt` file, rename the file at `$chipyard/generators/chipyard/src/main/scala/config/PackBitsDecompConfigs.scala.disabled` to `$chipyard/generators/chipyard/src/main/scala/config/PackBitsDecompConfigs.scala`. This will then register the `PackBitsConfig` so you can run simulations with the flag `CONFIG=PackBitsConfig`.
  * Look at [this file](/lab1/assets/03_building_custom_socs.pdf) for a conference demo/guide on how to integrate a RoCC accelerator into Chipyard's build system. 

Additional Debugging Tip: You can print something to the `.out` file during simulation in your Chisel RTL with the following line:
`PackBitsAccLogger.logInfo("TEXT: %x\n", Chisel data)`

See an example in `$chipyard/generators/packbits-acc/src/main/scala/PackBitsMemLoader.scala` Lines 44 - 53.

Deliverables: see [here](https://docs.google.com/document/d/1viKR8vl9QMF1Z8KPHJBwYqC1Y6W0rQxeLEIpcXNWxoE/edit?tab=t.0#heading=h.rqd04dy49y1h).

An extended Hardware Architectural Specification is available [here](https://docs.google.com/document/d/1viKR8vl9QMF1Z8KPHJBwYqC1Y6W0rQxeLEIpcXNWxoE/edit?tab=t.0). This is a long document with probably way more information (and quite a bit of background/non-absolutely necessary information) than you need.

As this class's ongoing effort to mimic what you may experience in research or industry, this spec was written to give you an idea of what a hardware architecture specification that defines the hardware & software interface may look like in industry. 

There is definitely still quite a degree of difference here as we are working with quite a small design & therefore there was a good degree of filler content, but it was written with inspiration of what [NVIDIA has published for their Deep Learning Accelerator](https://nvdla.org/hw/v1/hwarch.html) and what staff has seen from industry design experience.

In a large design, a well developed specification that defines how software should interact with hardware is critical in ensuring that the software folks that will be writing software for a custom piece of hardware is aware of how to interface and effectively use it. It also provides critical information to the hardware and microarchitecture design teams on how they should implement the hardware so that functionality and performance metrics are met.

Note: A hardware architecture specification is not a microarchitecture specification. It does not define how the hardware should be implemented. It only aims to cover the software & hardware interface. That is on purpose here as you should be the main designer for the microarchitecture.

## **Staggered release of hints**

We realize that implementing this accelerator can be extremely challenging & quite a daunting project to take on for a lab. While we want to pose a challenge for you to learn as much as possible about Chipyard, writing RTL, Chisel, integrating accelerators, baremetal testing, etc., the intention is **not** to burn you out.

Please come to office hours or ask TAs questions whenever needed.

We will also be gradually releasing more and more hints as we approach the due date. Starting from ~1 week before the due date, we will release solutions to help everyone integrate their accelerator into Chipyard so they can get a waveform.

Then solutions for the Command Router will be released a few days later.

Finally, we will release hints and potentially a solution to deal with handling the 256-bit DMA beat in the actual Decompressor module, so all you need to focus on would be the FSM for decompression, which should be relatively easy once you are able to access & process data at the individual byte level.

**However, if you want to be on the RTL team, we strongly suggest you implement this without waiting for the hints.** If you are on the RTL team, you will be implementing a design that is a lot more complex than this with a lot more moving parts. Getting used to the long specs (yes, there is a high chance we'll have a fully developed spec just like the one above for the actual tapeout design), figuring out what existing Chipyard/Chisel IP does and how to effectively debug are all essential skills that will help you as an RTL designer later in the class.

<!-- ## Integrating our Accelerator

Now that our accelerator works, it is time to incorporate it into an SoC. We do this by:
1. Defining a config fragment for our accelerator
1. Defining a new config that uses this config fragment

Inside `$chipyard/generators/custom-acc-rocc`, inspect `src/main/scala/configs.scala`. `WithCustomAccRoCC` is our config fragment here.

Answer the following questions:

**2. What does `p` do here? (Think about how it could be used, consider the object-oriented, generator-based style of writing, and feel free to look through other generators in Chipyard for examples.)**

**3. Give the 7-bit opcode used for instructions to our accelerator. Searching for the definition of `OpcodeSet` will be useful.**

We want to add our accelerator to a simple SoC that uses Rocket. To do this, we must make our config fragment accessible inside the chipyard generator. Open `$chipyard/build.sbt`. At line 161, add `custom_acc_rocc` to the list of dependencies of the chipyard project.

Next, navigate to `$chipyard/generators/chipyard/src/main/scala/config/RocketConfigs.scala`. **Define `CustomAccRoCCConfig`** such that it adds our accelerator to `RocketConfig`. The previous step made `customAccRoCC` available as a package here.

Hint: `CustomAccRoCCConfig` should look like the following:
```
class CustomAccRoCCConfig extends Config(
  /* YOUR CODE HERE */
)
``` -->

## Acknowledgements

Thank you to the whole Chipyard dev team for figures and documentation on Chipyard, and to Daniel Grubb for authorship of the original tutorial on which this lab is based.

Additionally, a huge thanks to Ethan Gao for a good chunk of the Chipyard overview content.
 <!--
## VLSI Flow

### Design Elaboration

The Hammer flow we have used throughout the semester is integrated into Chipyard.
A project setup similar to the ones we have previously used is in `chipyard/vlsi`.
To set up VLSI back-end design, run:

```
cd chipyard
scripts/init-vlsi.sh intech22
cd chipyard/vlsi
source hammer/sourceme.sh
```

**Note: For all compute intensive commands in the VLSI flow (all make commands from this point forwards, as well as launching Innovus via `open_chip`), run them on the LSF.** In other words, prepend the command with

```
bsub -Is
```

To setup the Hammer back-end flow, run:

```
make CONFIG=RocketConfig TOP=RocketTile tech_name=intech22 INPUT_CONFS="rockettile.yml" buildfile
```

Lets go through the various flags in this command:

- `CONFIG` sets the system config, the same way we set the config for RTL simulation
- `TOP` specifies the name of the module which will be the "top" module in our flow. The actual "top" of a design is `ChipTop`, but for this example, we will choose the `RocketTile` sub-module to run the flow
- `tech_name` specifies the target technology. You many want to edit the default setting for this in `vlsi/Makefile`, instead of specifying it each time you run a VLSI command
- `INPUT_CONFS` specifies a list of input YAML files which specify settings for HAMMER and the VLSI tools. You may want to edit the default setting for this in `vlsi/Makefile`, instead of specifying it each time you run a VLSI command
  - `rockettile.yml` specifies design-specific settings, in this case settings for running the VLSI flow when `RocketTile` is the top module
- The`buildfile` target describes a Makefile fragment that will be used in the actual VLSI flow (syn/par/drc/lvs), which we will run next. After this command runs, the generated verilog for the design should appear in the `vlsi/generated-src` directory.

One important difference between the `make` command for the VLSI flow, compared with the `make` command for the RTL simulation flow, is that in the VLSI flow the memories in the design will be mapped to hard SRAM macros available in the Hammer technology library. In the `generated-src/chipyard.TestHarness.RocketConfig` directory, inspect the file with the `.top.mems.conf` and `.top.mems.v` extensions. These files describe the parameters of the memories in the design, as well as the actual verilog instantiations of each memory.

**Q: What is the breakdown of SRAM blocks for each of the memories in the design? (this can be found by looking at the files described above.)**

### Synthesis

Now that the design is elaborated, we can leverage the Hammer infrastructure we have used this semester to physically build our system in much the same way as before.
Our Hammer config is in `rockettile.yml`. Here you can see we have again constrained our top-level clock to be 50 MHz. It is pretty straightforward to close timing for the Rocket core in the 100's of MHz with limited physical design input using Hammer out-of-the-box, but we are running it at this lower frequency to ease our design constraints.

To run synthesis, run:

```
make CONFIG=RocketConfig TOP=RocketTile tech_name=intech22 INPUT_CONFS="rockettile.yml" syn
```

This step should take up to about 1 hour.  If you are ssh'd directly into the machine (not using X2go, etc.), you should use a utility like `tmux` to make sure that you don't lose your run if you lose your connection or log off.
When it completes, you can look at the results just like before in `syn-rundir/reports/` to confirm your design passed timing.

**Q: What is the critical path in the design after synthesis? This can be found by inspecting the timing reports in the directory described above.**



### Place-and-route

The next step is to run place-and-route on the synthesized netlist.

```
make CONFIG=RocketConfig TOP=RocketTile tech_name=intech22 INPUT_CONFS="rockettile.yml" par
```

This step will also be pretty slow.
You can open up the final design in Innovus using `par-rundir/generated-scripts/open_chip`.

#### Floorplanning

Floorplanning is a key step to all designs and will have a huge effect on your design's QoR. Hammer's placement constraints API provides several options for controlling the floorplan. You can look in `rockettile.yml` to see how these constraints are being used.

- "placement" constraints constrain the position of an instance. For example, the constraints on the positions of the FPU and Core modules within the RocketTile. These constraints are more like guidelines to the tool, rather than restrictions
- "hardmacro" constraints constrain the position of hard macros. You can see we constrain the position of all the SRAM macros in the design
- "hierarchical" constraints are used in the Hammer hierarchical flow, where sub-modules of the design are individually place-and-routed
- "obstruciton" constraints can block placement of standard cells, routing, or power straps.

In this case, we should adjust the placement of the SRAM macros for our Rocket core's L1 caches.
In these Hammer constraints, you can see that the lower-left hand corner placement is specified (in microns) as well as the orientation (see `hammer/src/hammer-vlsi/defaults.yml` for documentation on all placement options).

Take a look at the layout, and notice where an obvious improvement in the floorplan can be made. Modify the specification in `rockettile.yml`.

After editing an input yml/json file, the Hammer Make include file will detect this and re-run the flow. This means that any change to any yml/json will rerun the entire syn-pnr flow after a change to `rockettile.yml`.

If you want to rerun only part of the flow (for example, only par), you have to use special `redo-STEP` flags and `HAMMER_EXTRA_ARGS`. This is because Hammer does not know which config options only affect place-and-route, so to be safe, the Hammer Makefile will rerun all prerequisite steps.

In our case, modifying the floorplan does not affect synthesis at all, so we can make the informed decision to avoid rerunning synthesis. Note that should always be done with great caution, as otherwise, changes in your design or config may not propagate to your time-consuming job.

To rerun only place-and-route after editing only floorplan constraints, run

```
make CONFIG=RocketConfig TOP=RocketTile tech_name=intech22 INPUT_CONFS="rockettile.yml" HAMMER_EXTRA_ARGS="-p rockettile.yml" redo-par
```

**Q: Include a picture of your design in Innovus with the top two metal layers turned off.**

**Q: Explain your modification to the provided floorplan, and show it in the layout.**

**Q: How much setup timing slack is there in the design?**

**Q: Include a picture of the clock tree debugger for your design from Innovus and comment on the balancing.**

#### Hierarchical Flows

In the actual class tapeout, and in most large tapeouts, the flow is hierarchical. Meaning that subcomponents will be place-and-routed as blocks, before the parent module places them as macros. If we were to do a two-level hierarchical flow, where the RocketTile is the only child module, the next step would be to synthesize and place-and-route the parent ChipTop module.

### DRC

Running DRC (Design rule checks) verifies that the layout emitted after place-and-route adheres to all the foundry rules, and is manufacturable.

Note: The example design is not intended to be DRC-clean.

```
make CONFIG=RocketConfig TOP=RocketTile tech_name=intech22 INPUT_CONFS="rockettile.yml" drc-block
```


### LVS

Running LVS (layout vs. schematic) verifies that the netlist in the final layout matches the expected netlist.

Note: The example design is not intended to be LVS-clean.

```
make CONFIG=RocketConfig TOP=RocketTile tech_name=intech22 INPUT_CONFS="rockettile.yml" lvs-block
```

-->

<!-- ## Rest of the VLSI Flow -->

<!-- Running DRC and LVS is not required for this lab, but you can run them though Hammer just like before. -->
<!-- The placement of macros like SRAMs can cause considerable numbers of DRC and LVS errors if placed incorrectly and can cause considerable congestion if placed non-optimally. -->
<!-- The floorplan visualization tools in Hammer can help you root out these problems early in your design process. -->

<!---

## Conclusion

Chipyard is designed to allow you to rapidly build and integrate your design with general purpose control and compute as well as a whole host of other generators.
You can then take your design, run some RTL simulations, and then push it through the VLSI flow with the technology of your choice using Hammer.
The tools integrated with Chipyard, from how you actually build your design (eg. Chisel and generators), to how you verify and benchmark its performance, to how you physically implement it, are meant to enable higher design QoR within an agile hardware design process through increased designer productivity and faster design iteration.
We just scratched the surface in this lab, but there are always more interesting features being integrated into Chipyard.
We recommend that you continue to explore what you can build with Chipyard given this introduction!

## Acknowledgements

Thank you to the whole Chipyard dev team for figures and documentation on Chipyard, and to Daniel Grubb for authorship of the original tutorial on which this lab is based.

-->
