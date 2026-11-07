from q2b import count_non_digits

print()
print('-' * 20)
print()

print("Test Case 1: count_non_digits(['123456', '13579', '24680', '888'])")

print()

result = count_non_digits(['123456', '13579', '24680', '888']) 
print('Expected: [0, 0, 0, 0]')
print('Actual:   ' + str(result))

print("\nExpected type of returned value: <class 'list'>")
print('Actual type of returned value:   ' + str(type(result)))

print()
print('-' * 20)
print()

print("Test Case 2: count_non_digits(['abc', 'Hello Python!', ''])")

print()

result = count_non_digits(['abc', 'Hello Python!', ''])
print('Expected: [3, 13, 0]')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 3: count_non_digits(['1-2-3-4-5-6', '32!*-4*5*6-', '2023-24-T1-SMU-SCIS'])")

print()

result = count_non_digits(['1-2-3-4-5-6', '32!*-4*5*6-', '2023-24-T1-SMU-SCIS'])
print('Expected: [5, 6, 12]')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 4: count_non_digits(['', '', ''])")

print()

result = count_non_digits(['', '', ''])
print('Expected: [0, 0, 0]')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 5: count_non_digits([])")

print()

result = count_non_digits([])
print('Expected: []')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()