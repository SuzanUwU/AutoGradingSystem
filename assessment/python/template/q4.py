# Name:
# Email ID:

# ###############################################################################
# The function : sort_by_latest_delivery_time() is GIVEN.
# DO NOT remove or modify the function sort_by_latest_delivery_time().
# You MUST use this function in your solution.
# ###############################################################################

def sort_by_latest_delivery_time(items):
    """
    Sorts a list of items in an increasing order of their latest_delivery_time.

    This function takes in a list of items, where each item is a tuple containing
    the item_name, destination, latest_delivery_time, travel_time of item, and sorts 
    them in ascending order of their latest_delivery_time.
    The latest_delivery_time is assumed to be the third element in each item.
	If there is more than one item with the same latest_delivery_time, this function 
    will keep them in the same order as specified in the initial list of items.

    Parameter:
        items (list): A list of items to be sorted.

    Returns: None
        Note that This function sorts the given list items and DOES NOT return a new list.

    """
    for i in range(1, len(items)):
        key = items[i]
        j = i - 1
        while j >= 0 and key[2] < items[j][2]:
            items[j + 1] = items[j]
            j -= 1
        items[j + 1] = key

# ###############################################################################

def schedule_delivery(items):
    # Replace the code below with your implementation.

    return None
    