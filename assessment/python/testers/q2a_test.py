from q2a import get_digits_of_even_index

print()
print('-' * 20)
print()

print("Test Case 1: get_digits_of_even_index('123456')")

print()

result = get_digits_of_even_index('123456')
print('Expected: 135')
print('Actual:   ' + str(result))

print("\nExpected type of returned value: <class 'str'>")
print('Actual   type of returned value: ' + str(type(result)))

print()
print('-' * 20)
print()

print("Test Case 2: get_digits_of_even_index('SMU-1-2-3-4')")

print()

result = get_digits_of_even_index('SMU-1-2-3-4')
print('Expected: 1234')
print('Actual:   ' + str(result))


print()
print('-' * 20)
print()

print("Test Case 3: get_digits_of_even_index('32!*-4*5(0)36')")

print()

result = get_digits_of_even_index('32!*-4*5(0)36')
print('Expected: 36')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 4: get_digits_of_even_index('2023SMU-SCIS1')")

print()

result = get_digits_of_even_index('2023SMU-SCIS1')
print('Expected: 221')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 5: get_digits_of_even_index('SMU-SCIS')")

print()

result = get_digits_of_even_index('SMU-SCIS')
print('Expected: **')
print('Actual:   ' + '*' + str(result) + '*')

print()
print('-' * 20)
print()

print("Test Case 6: get_digits_of_even_index('SCIS-1')")

print()

result = get_digits_of_even_index('SCIS-1')
print('Expected: @@')
print('Actual:   ' + '@' + str(result) + '@')

print()
print('-' * 20)
print()

print("Test Case 7: get_digits_of_even_index('')")

print()

result = get_digits_of_even_index('')
print('Expected: ##')
print('Actual:   ' + '#' + str(result) + '#')

print()
print('-' * 20)
print()