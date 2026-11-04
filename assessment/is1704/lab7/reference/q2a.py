# Q2a: Universities 
# Solution
# ########################################################################
def get_universities_founded_before(filename, given_year):
    list_to_return = []
    with open(filename, 'r') as my_file:
        for line in my_file:
            line = line.rstrip('\n')
            columns = line.split('\t')
            name = columns[0]
            year = int(columns[2])
            if year < given_year:
                list_to_return.append(name)
        return list_to_return


