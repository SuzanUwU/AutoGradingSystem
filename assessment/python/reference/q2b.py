# Name:
# Email ID:

def count_non_digits(str_list):
    # Modify the code below
    list_to_return = []
    for s in str_list:
        count = 0
        for ch in s:
            if not ch.isdigit():
                count += 1
        list_to_return.append(count)
    return list_to_return       