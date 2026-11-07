from q3c import make_tables_fully_compliant
from q3b import is_fully_compliant


def frame_str(my_str):
    line = '*' * (len(my_str) + 4)
    print('\t\t' + line)
    print('\t\t' + '* ' + my_str + ' *')
    print('\t\t' + line)


def test_part_c(t1,  t2):
    # initial compliance of the 2 tables
    print("First table fully compliant:  " + str(is_fully_compliant(t1)))
    print("Second table fully compliant: " + str(is_fully_compliant(t2)))

    result = make_tables_fully_compliant(t1, t2)

    if result == None:
        # default value returned by the function
        frame_str("Swap done: " + str(result))
    else:
        # No possible swap was found
        if result == "not possible":
            frame_str("Swap done: " + str(result))
        else:
            # Swap has been possible
            change, new_t1, new_t2 = result
            frame_str("Swap done: " + str(change))
            print("First table fully compliant:  " + str(is_fully_compliant(new_t1)))
            print("Second table fully compliant: " + str(is_fully_compliant(new_t2)))


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
print('=' * 20)
print()
print("Test Case 1: test_part_c(table_12, table_13)")
print()
print('Expected:')
print('---------')
print("First table fully compliant:  False\nSecond table fully compliant: False")
print("\t\t*****************************")
print("\t\t* Swap done: ('A12', 'A13') *")
print("\t\t*****************************")
print("First table fully compliant:  True\nSecond table fully compliant: True")
print('\nActual:')
print('-------')
test_part_c(table_12, table_13)
print()
print('=' * 20)

print()
print("Test Case 2: test_part_c(table_10, table_12))")
print()
print('Expected:')
print('---------')
print("First table fully compliant:  True\nSecond table fully compliant: False")
print("\t\t*****************************")
print("\t\t* Swap done: ('C10', 'A12') *")
print("\t\t*****************************")
print("First table fully compliant:  True\nSecond table fully compliant: True")
print('\nActual:')
print('-------')
test_part_c(table_10, table_12)
print()
print('=' * 20)

print()
print("Test Case 3: test_part_c(table_11, table_12))")
print()
print('Expected: ')
print('---------')
print("First table fully compliant:  False\nSecond table fully compliant: False")
print("\t\t***************************")
print("\t\t* Swap done: not possible *")
print("\t\t***************************")

print('\nActual:')
print('-------')
test_part_c(table_11, table_12)
print()
print('=' * 20)
print()
