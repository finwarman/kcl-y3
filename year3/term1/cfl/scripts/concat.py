#! /usr/bin/env python3

# helper function to raise languages to the nth power
# langpower(a, b, 2) can be used to concat two separate languages


def lang_power(result, base, power):
    if power <= 1:
        return result
    else:
        new_result = set()
        for a in result:
            for b in base:
                new_result.add(a + b)
        print(len(new_result))
        return lang_power(sorted(list(new_result)), base, power-1)

#  Usage:

# a = ['a', 'b', 'c', '']
# result = lang_power(a, a, 4)

# result = lang_power(
#     ['a', 'b', 'c', 'd'],
#     ['1', '2', '3', '4', '5', '6', '7'],
#     2
# )


result = lang_power(
    ['1', '11', '111', '1111'],
    ['1', '11', '1111', '11111', '11111', '111111', '111111'],
    2
)

print('\n')
print(result)
print(len(result))
