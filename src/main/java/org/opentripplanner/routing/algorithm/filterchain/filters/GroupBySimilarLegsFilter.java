package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByTripIdAndDistance;

public class GroupBySimilarLegsFilter extends GroupByFilter<GroupByTripIdAndDistance> {


  /**
   * This filter will group the itineraries by the main-legs and reduce the number of itineraries
   * in each group. The main legs is the legs that together constitute more than a given
   * percentage of the total travel distance.
   * <p>
   * Each group is filtered using generalized-cost, keeping only the itineraries with the lowest
   * cost. If there is a tie, the filter look at the number-of-transfers as a tie breaker.
   * <p>
   * The number of itineraries kept in each group depend on the desired min-limit. If the
   * min-limit is 10, and the number of groups is 5, then each group is reduced to 2 elements.
   * <p>
   * The filter name is dynamically created: similar-legs-filter-68p-1
   *
   * @param groupByP                   The percentage of the distance the main-legs must
   *                                   constitute to make up the "main" part of the itinerary.
   * @param maxNumOfItinerariesPerGroup The number of elements we reduce each group down to.
   * @param sortOrder                  Each group is sorted using the {@code sortOrder}, before
   *                                   elements are removed from the tail of the sorted list.
   */
  public GroupBySimilarLegsFilter(double groupByP, int maxNumOfItinerariesPerGroup, SortFilter sortOrder) {
    super(
        name(groupByP, maxNumOfItinerariesPerGroup),
        it -> new GroupByTripIdAndDistance(it, groupByP),
        sortOrder,
        maxNumOfItinerariesPerGroup
    );
  }

  private static String name(double groupByP, int maxNumOfItinerariesPerGroup) {
    return "similar-legs-filter-" + (int)(100d * groupByP) + "p-" + maxNumOfItinerariesPerGroup + "x";
  }
}
