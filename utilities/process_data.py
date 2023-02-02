#!/usr/bin/env python3

#
#   This script export a file in azure table storage format
#   input: file path, partition key name, row key name
#   output: processed file
#

import sys
import pandas as pd


def swap_cols(df, col_name, index):
    col_list = list(df.columns)
    x, y = col_list.index(col_name), index

    if x == y:
        return df
    else:
        col_list[y], col_list[x] = col_list[x], col_list[y]
        df = df[col_list]
        return df


def main(filepath, partition_key, row_key):
    df = pd.read_csv(filepath, dtype=str)

    df = swap_cols(df, partition_key, 0)
    df = swap_cols(df, row_key, 1)

    df.rename(columns = {partition_key:'PartitionKey'}, inplace = True)
    df.rename(columns = {row_key:'RowKey'}, inplace = True)

    df.to_csv('processed.csv', header=True, index=False)



if __name__ == '__main__':
    args = len(sys.argv)
    if args < 4:
        print("\nIncorrect number of arguments")
        print("Usage: python3 " + sys.argv[0] + " <file-path> <partition-key> <row-key>")
    else:
        main(filepath=sys.argv[1], partition_key=sys.argv[2], row_key=sys.argv[3])