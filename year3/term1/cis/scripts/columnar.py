#! /usr/bin/env python3

import string
import math

message = "attack postponed until Two AM"
key = "4312567"
key = [int(c) for c in key]
padding = "X"
stages = 2

def format_pad_message(msg, key, padding):
    message = msg.replace(" ", "").upper()
    full_length = int(math.ceil(len(message) / len(key))) * len(key)
    diff = full_length - len(message)
    padded_msg = message + (padding * diff)
    return padded_msg

def generate_grid(msg, key):
    rows = math.ceil(len(msg)/len(key))
    grid = [[' ' for c in range(len(key))] for _ in range(rows)]
    for y in range(rows):
        for x in range(len(key)):
            grid[y][x] = msg[(y*len(key))+x]
    return grid

def generate_ciphertext(grid, key):
    ciphertext = ""
    for i in range(len(key)):
        for row in range(len(grid)):
            c = key.index(i+1)
            ciphertext += grid[row][c]
    return ciphertext

def multistage_columnar(msg, key, padding, stages):
    msg = format_pad_message(message, key, padding)
    grid = generate_grid(msg, key)
    ciphertext = generate_ciphertext(grid, key)
    for _ in range(stages - 1):
        grid = generate_grid(ciphertext, key)
        ciphertext = generate_ciphertext(grid, key)
    return ciphertext

ciphertext = multistage_columnar(message, key, padding, stages)

print("".join(ciphertext))
