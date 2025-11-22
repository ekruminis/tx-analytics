# ZBRA TFM Simulator

Transaction Fee Mechanism Simulator for Miner Incentives Analysis

## Overview

The ZBRA-TFM simulator is a modular, discrete-event Java application designed to analyse transaction fee mechanisms (TFMs) in fee-only blockchain environments. It enables researchers to explore the effects of different fee mechanisms on miner incentives and user payouts using configurable simulations with real or synthetic transaction datasets.

## Features

- Simulates multiple transaction fee mechanisms, including:
  - First Price Auction (`first_price`)
  - Second Price Auction (`second_price`)
  - EIP-1559 (`eip1559`)
  - Reserve Pool TFM (`pool`)
  - Burning Second Price Auction (`burning_second_price`)
- Configurable number of miners with stake-weighted block selection
- Inputs JSON-formatted transaction datasets
- Outputs detailed and summary CSV log files
- Extensible framework to add new TFMs or customize consensus policies
- Command-line interface for batch simulation runs

## Installation

1. Requires Java 11 or newer.
2. Build the project using Maven or Gradle or download the executable `simulator.jar`.
3. Add your input transaction dataset file (JSON-formmated) in `\input` directory.

## Usage

Run the simulator with:

java -jar simulator.jar <TFM_STYLE> <NUMBER_OF_BLOCK_CYCLES> <SEED> <NUMBER_OF_MINERS> <OUTPUT_FILENAME> <INPUT_FILENAME>

Example:

java -jar simulator.jar second_price 144 89433 15 sp_logs txs-week.json

- `TFM_STYLE`: Choose from `first_price`, `second_price`, `eip1559`, `pool`, `burning_second_price`
- `NUMBER_OF_BLOCK_CYCLES`: Number of 10-minute block cycles to simulate
- `SEED`: Random seed value for reproducibility
- `NUMBER_OF_MINERS`: Number of miners in simulation
- `OUTPUT_FILENAME`: CSV file to write simulation logs in `output`
- `INPUT_FILENAME`: JSON file with transaction data in `input`

## Input Data Format

Input files must be JSON arrays with transaction objects. Each transaction should include relevant fields such as hash, fee, size and weight. Example input files are provided.

<pre>
[
  {
    "hash": "5d86d76eb840024d070508a771b1f65dbcd9a1ea5ca41830703faf7d8a83cc67",
    "fee": "0.00216065",
    "size": "225",
    "weight": "573"
  },
  {
    "hash": "a417b4909a231f69fad9e4901b262965510d5aa672f3415cf6c5e61feb786a5d7",
    "fee": "0.00150000",
    "size": "300",
    "weight": "600"
  }
]
</pre>

## Output Data Format

Outputs are CSV files summarising per-block transaction outcomes, miner payouts, and network performance for analysis.

### TFM Styles
- **first_price** -> 1st Price Auction TFM *(i.e. attach highest offering txs)*
- **second_price** -> 2nd Price Auction TFM *(i.e. attach highest offering txs, users pay lowest included gas_price bid)*
- **eip1559** -> EIP-1559 TFM *(i.e. attach txs based on dynamic base fee which is burned, only tips go to miner payout)*
- **pool** -> Reserve Pool TFM *(i.e. attach txs based on dynamic base fee, pay miners optimal payout (base_fee * block_target_size), give rest to the shared pool)*
- **burning_second_price** -> Burning 2nd Price Auction TFM *(i.e. attach highest offering txs, top N txs are confirmed, {block_size - N} txs are unconfirmed, users only pay the fee of the highest unconfirmed txs, miner payout is total of fees from unconfirmed txs, any surplus is burned.)* based on Chung/Shi paper - https://arxiv.org/pdf/2111.03151.pdf

## Architecture and Extensibility

- Modular design separates fee mechanism logic (`AbstractTFM` class) from core simulation.
- Extend `AbstractTFM` and override `fetchValidTX()` to add new transaction fee mechanisms.
- Modify `getWinningMiner()` in `Simulation.java` to change miner selection or consensus rules.
- Uses Gamma-Poisson distribution to model platform transaction arrival bursts.
- Pseudorandom seed ensures reproducibility of transaction sampling and miner selection.

## Contribution

Contributions and issue reports are welcome. Please open pull requests or issues on the GitHub repository.

## License

MIT License
