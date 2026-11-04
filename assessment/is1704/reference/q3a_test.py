from q3a import is_partially_compliant

table_1 = [ ('A1', 2021, 'scis', ['english', 'german'], ['data analysis', 'python']),
            ('B1', 2022, 'bus', ['english', 'french'], ['teamwork', 'oral communication']),
            ('C1', 2022, 'scis', ['chinese', 'english'], ['data analysis']),
            ('D1', 2021, 'bus', ['tamil','english', 'italian'], ['teamwork', 'time management'])]

table_2 = [ ('A2', 2020, 'scis', ['english', 'german'], ['data analysis', 'python']),
            ('B2', 2022, 'bus', ['english', 'french'], ['teamwork', 'oral communication']),
            ('C2', 2022, 'scis', ['chinese', 'english'], ['data analysis']),
            ('D2', 2020, 'bus', ['tamil','english', 'italian'], ['teamwork', 'time management'])]

table_3 = [ ('A3', 2020, 'scis', ['english', 'german'], ['data analysis', 'python']),
            ('B3', 2021, 'bus', ['english', 'french'], ['teamwork', 'oral communication']),
            ('C3', 2021, 'scis', ['chinese', 'english'], ['data analysis']),
            ('D3', 2022, 'bus', ['tamil','english', 'italian'], ['teamwork', 'time management'])]

table_4 = [ ('A4', 2023, 'scis', ['english', 'german'], ['data analysis', 'python']),
            ('B4', 2022, 'scis', ['english', 'french'], ['teamwork', 'oral communication']),
            ('C4', 2022, 'scis', ['chinese', 'english'], ['data analysis']),
            ('D4', 2022, 'bus', ['tamil','english', 'italian'], ['teamwork', 'time management'])]

print()
print('-' * 20)
print()

print("Test Case 1: is_partially_compliant(table_1)")

print()

result = is_partially_compliant(table_1)
print('Expected: True')
print('Actual:   ' + str(result))

print("\nExpected type of returned value: <class 'bool'>")
print('Actual   type of returned value: ' + str(type(result)))

print()
print('-' * 20)
print()

print("Test Case 2: is_partially_compliant(table_2)")

print()

result = is_partially_compliant(table_2)
print('Expected: False')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 3: is_partially_compliant(table_3)")

print()

result = is_partially_compliant(table_3)
print('Expected: False')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 4: is_partially_compliant(table_4)")

print()

result = is_partially_compliant(table_4)
print('Expected: False')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()