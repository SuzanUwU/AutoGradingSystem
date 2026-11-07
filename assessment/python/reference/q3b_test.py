from q3b import is_fully_compliant

table_10 = [ ('A10', 2021, 'scis', ['english', 'german'], ['data analysis', 'teamwork', 'problem solving', 'python']),
            ('B10', 2022, 'bus', ['english', 'french'], ['teamwork', 'problem solving', 'oral communication']),
            ('C10', 2022, 'scis', ['chinese', 'english'], ['data analysis', 'problem solving', 'python', 'java']),
            ('D10', 2021, 'bus', ['tamil', 'english', 'italian'], ['teamwork', 'time management', 'data analysis'])]

table_11 = [ ('A11', 2021, 'scis', ['english', 'german'], ['data analysis', 'teamwork', 'problem solving', 'python']),
            ('B11', 2022, 'bus', ['english', 'french'], ['teamwork', 'problem solving', 'oral communication']),
            ('C11', 2023, 'scis', ['chinese', 'english'], ['data analysis', 'problem solving', 'python', 'java']),
            ('D11', 2021, 'bus', ['tamil', 'english', 'italian'], ['teamwork', 'time management', 'data analysis'])]

table_12 = [ ('A12', 2021, 'scis', ['english', 'chinese'], ['data analysis', 'teamwork', 'problem solving', 'python']),
            ('B12', 2022, 'bus', ['english', 'french'], ['teamwork', 'problem solving', 'oral communication']),
            ('C12', 2022, 'scis', ['chinese', 'english'], ['data analysis', 'problem solving', 'python', 'java']),
            ('D12', 2021, 'bus', ['tamil', 'english', 'italian'], ['teamwork', 'time management', 'data analysis', 'python'])]

table_13 = [ ('A13', 2021, 'scis', ['english', 'german'], ['data analysis', 'python']),
            ('B13', 2022, 'bus', ['chinese', 'french'], ['teamwork', 'problem solving', 'oral communication']),
            ('C13', 2022, 'scis', ['chinese', 'english'], ['data analysis', 'problem solving', 'python', 'java']),
            ('D13', 2021, 'bus', ['tamil', 'english', 'italian'], ['teamwork', 'time management', 'data analysis'])]                 


print()
print('-' * 20)
print()

print("Test Case 1: is_fully_compliant(table_10)")

print()

result = is_fully_compliant(table_10)
print('Expected: True')
print('Actual:   ' + str(result))

print("\nExpected type of returned value: <class 'bool'>")
print('Actual   type of returned value: ' + str(type(result)))

print()
print('-' * 20)
print()

print("Test Case 2: is_fully_compliant(table_11)")

print()

result = is_fully_compliant(table_11)
print('Expected: False')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 3: is_fully_compliant(table_12)")

print()

result = is_fully_compliant(table_12)
print('Expected: False')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()

print("Test Case 4: is_fully_compliant(table_13)")

print()

result = is_fully_compliant(table_13)
print('Expected: False')
print('Actual:   ' + str(result))

print()
print('-' * 20)
print()