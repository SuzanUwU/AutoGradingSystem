# Name:
# Email ID:

def get_digits_of_even_index(my_str):
    # Modify the code below.
    str_to_return = ''
    for i in range(len(my_str)):
        ch = my_str[i]
        if i%2 == 0 and ch.isdigit():
            str_to_return += ch
    return str_to_return