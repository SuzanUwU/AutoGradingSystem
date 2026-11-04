# Q2c: Universities 
# Solution
# ########################################################################
def search_with_keyword(filename, keyword):
    list_to_return = []
    with open(filename, 'r') as my_file:
        for line in my_file:
            line = line.rstrip('\n')
            columns = line.split('\t')
            name = columns[0]
            acronym = columns[1]
            if keyword.lower() in name.lower():
                list_to_return.append(acronym)
    return list_to_return



