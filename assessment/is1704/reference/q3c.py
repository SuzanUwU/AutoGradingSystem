# Name:
# Email ID:

from q3b import is_fully_compliant

def make_tables_fully_compliant(t1, t2):
    # Replace the code below with your implementation.
    for i in range(len(t1)):
        st1 = t1[i]
        for j in range(len(t2)):
            st2 = t2[j]
            new_t1 = t1[0:i] + [st2] + t1[i+1:]
            new_t2 = t2[0:j] + [st1] + t2[j+1:]
            if is_fully_compliant(new_t1) and is_fully_compliant(new_t2):
                return ((st1[0],st2[0]), new_t1, new_t2)
    return "not possible"
    