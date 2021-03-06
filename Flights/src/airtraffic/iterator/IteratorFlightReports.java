package airtraffic.iterator;

import static airtraffic.iterator.AccumulatorHelper.accumulate;
import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry.comparingByKey;
import static java.util.Map.Entry.comparingByValue;
import static org.apache.commons.lang3.StringUtils.left;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import airtraffic.Airport;
import airtraffic.Carrier;
import airtraffic.Flight;
import airtraffic.FlightDistanceRange;
import airtraffic.FlightReports;
import airtraffic.PairGroup;
import airtraffic.ReportContext;
import airtraffic.Route;

/**
 * Generate various flight statistics using Java iterators.
 *
 * @author tony@piazzaconsulting.com
 */
public class IteratorFlightReports implements FlightReports {
   private static final Comparator<Flight> FLIGHT_DISTANCE_COMPARATOR = 
      new Comparator<Flight>() {
         @Override public int compare(Flight f1, Flight f2) {
            return f1.getDistance() - f2.getDistance();
         }
      };
   private static final List<FlightDistanceRange> DISTANCE_RANGES =
      Arrays.asList(FlightDistanceRange.between(   0,  100), 
                    FlightDistanceRange.between( 101,  250),
                    FlightDistanceRange.between( 251,  500),
                    FlightDistanceRange.between( 501, 1000),
                    FlightDistanceRange.between(1001, 2500),
                    FlightDistanceRange.between(2501, 5000),
                    FlightDistanceRange.between(5001, 9999));

   public void reportTotalFlightsFromOrigin(ReportContext context) {
      final int year = context.getYear();
      final Airport origin = context.getOrigin();

      long count = 0;
      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      while(iterator.hasNext()) {
         Flight flight = iterator.next();
         if(flight.notCancelled() && flight.getOrigin().equals(origin)) {
            ++count;
         }
      }

      context.getTerminal()
             .printf("Total flights from %s is %,d\n", 
                     origin.getName().trim(), 
                     count);
   }

   public void reportTotalFlightsToDestination(ReportContext context) {
      final int year = context.getYear();
      final Airport destination = context.getDestination();

      long count = 0;
      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      while(iterator.hasNext()) {
         Flight flight = iterator.next();
         if(flight.notCancelled() && flight.notDiverted() &&  
            flight.getDestination().equals(destination)) {
            ++count;
         }
      }

      context.getTerminal()
             .printf("Total flights to %s is %,d\n", 
                     destination.getName().trim(), 
                     count);
   }

   public void reportTotalFlightsFromOriginToDestination(ReportContext context) {
      final int year = context.getYear();
      final Airport origin = context.getOrigin();
      final Airport destination = context.getDestination();

      long count = 0;
      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      while(iterator.hasNext()) {
         Flight flight = iterator.next();
         if(flight.notCancelled() && flight.notDiverted() &&  
            flight.getOrigin().equals(origin) &&
            flight.getDestination().equals(destination)) {
            ++count;
         }
      }

      context.getTerminal()
             .printf("Total of %,d flights from %s (%s)\nto %s (%s)\n", 
                     count,
                     origin.getName().trim(), 
                     origin.getIATA(), 
                     destination.getName().trim(), 
                     destination.getIATA()
      ); 
   }

   public void reportTopFlightsByOrigin(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new CountingAccumulator<Flight, Airport>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public Airport getKey(Flight source) {
               return source.getOrigin();
            }
            @Override public void forEach(Entry<Airport, Long> entry) {
               context.getTerminal()
                      .printf("%3s\t\t%,10d\n", 
                              entry.getKey().getIATA(), 
                              entry.getValue());
            }
         }
      );
   }

   public void reportTopDestinationsFromOrigin(ReportContext context) {
      final int year = context.getYear();
      final Airport origin = context.getOrigin();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new CountingAccumulator<Flight, Airport>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled() && 
                      source.getOrigin().equals(origin);
            }
            @Override public Airport getKey(Flight source) {
               return source.getDestination();
            }
           @Override public void forEach(Entry<Airport, Long> entry) {
              context.getTerminal()
                     .printf("%3s\t\t%,10d\n", 
                             entry.getKey().getIATA(), 
                             entry.getValue());
            }
         }
      );
   }

   public void reportMostPopularRoutes(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new CountingAccumulator<Flight, Route>() {
            @Override public boolean filter(Flight source) {
               return true;
            }
            @Override public Route getKey(Flight source) {
               return source.getRoute();
            }
            @Override public void forEach(Entry<Route, Long> entry) {
               context.getTerminal()
                      .printf("%s\t%,10d\n", 
                              entry.getKey(), 
                              entry.getValue().intValue());
            }
         }
      );
   }

   public void reportWorstAverageDepartureDelayByOrigin(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new MapAccumulator<Flight, Airport, AverageValue>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public Airport getKey(Flight source) {
               return source.getOrigin();
            }
            @Override public AverageValue initializeValue(Flight source) {
               return new AverageValue(source.getDepartureDelay());
            }
            @Override public AverageValue updateValue(Flight source, AverageValue value) {
               return value.add(source.getDepartureDelay());
            }
            @Override public void forEach(Entry<Airport, AverageValue> entry) {
               context.getTerminal()
                      .printf("%3s\t\t%.0f\n", 
                              entry.getKey().getIATA(), 
                              entry.getValue().getAverage());
            }
         }
      );
   }

   public void reportWorstAverageArrivalDelayByDestination(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new MapAccumulator<Flight, Airport, AverageValue>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public Airport getKey(Flight source) {
               return source.getDestination();
            }
            @Override public AverageValue initializeValue(Flight source) {
               return new AverageValue(source.getArrivalDelay());
            }
            @Override public AverageValue updateValue(Flight source, AverageValue value) {
               return value.add(source.getArrivalDelay());
            }
            @Override public void forEach(Entry<Airport, AverageValue> entry) {
               context.getTerminal()
                      .printf("%3s\t\t%.0f\n", 
                              entry.getKey().getIATA(), 
                              entry.getValue().getAverage());
            }
         }
      );
   }

   public void reportMostCancelledFlightsByOrigin(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new CountingAccumulator<Flight, Airport>() {
            @Override public boolean filter(Flight source) {
               return source.cancelled();
            }
            @Override public Airport getKey(Flight source) {
               return source.getOrigin();
            }
            @Override public void forEach(Entry<Airport, Long> entry) {
               context.getTerminal()
                      .printf("%3s\t\t%,8d\n", 
                              entry.getKey().getIATA(), 
                              entry.getValue());
            }
         }
      );
   }

   public void reportTotalFlightsByOriginState(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new CountingAccumulator<Flight, String>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public String getKey(Flight source) {
               return source.getOrigin().getState();
            }
            @Override public void forEach(Entry<String, Long> entry) {
               context.getTerminal()
                      .printf("%2s\t%,10d\n", 
                              entry.getKey(), 
                              entry.getValue());
            }
         }
      );
   }

   public void reportTotalFlightsByDestinationState(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new CountingAccumulator<Flight, String>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public String getKey(Flight source) {
               return source.getDestination().getState();
            }
            @Override public void forEach(Entry<String, Long> entry) {
               context.getTerminal()
                      .printf("%2s\t%,10d\n", 
                              entry.getKey(), 
                              entry.getValue());
            }
         }
      );
   }

   public void reportLongestFlights(ReportContext context) {
      byDistance(context, FLIGHT_DISTANCE_COMPARATOR.reversed());
   }

   public void reportShortestFlights(ReportContext context) {
      byDistance(context, FLIGHT_DISTANCE_COMPARATOR);
   }

   private void byDistance(ReportContext context, Comparator<Flight> comparator) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      List<Flight> flights = new ArrayList<>();
      while(iterator.hasNext()) {
         Flight flight = iterator.next();
         if(flight.notCancelled() && flight.notDiverted()) {
            flights.add(flight);
         }
      }
      Collections.sort(flights, comparator);
      int count = 0;
      for(Flight flight : flights) {
         context.getTerminal()
                .printf("%-8s  %10s\t  %2s\t %3s\t    %3s\t\t%6d\n", 
                        flight.getFlightNumber(),
                        flight.getDate(),
                        flight.getCarrier().getCode(),
                        flight.getOrigin().getIATA(),
                        flight.getDestination().getIATA(),
                        flight.getDistance()
                );
         if(++count >= limit) {
            break;
         }         
      }
      flights.clear();
   }

   public void reportTotalFlightsByDistanceRange(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByKey(), limit, 
         new MapAccumulator<Flight, FlightDistanceRange, Long>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled() && source.notDiverted();
            }
            @Override public FlightDistanceRange getKey(Flight source) {
               int distance = source.getDistance();
               for(FlightDistanceRange range : DISTANCE_RANGES) {
                  if(range.contains(distance)) {
                     return range;
                  }
               }
               throw new IllegalStateException("No range for distance of " + distance);
            }
            @Override public Long initializeValue(Flight source) {
               return Long.valueOf(1);
            }
            @Override public Long updateValue(Flight source, Long value) {
               return Long.valueOf(value.longValue() + 1);
            }
            @Override public void forEach(Entry<FlightDistanceRange, Long> entry) {
               context.getTerminal()
                      .printf("%-10s\t%,10d\n", 
                              entry.getKey(), 
                              entry.getValue());
            }
         }
      );
   }

   public void reportDaysWithLeastCancellations(ReportContext context) {
      byDaysWithCancellations(context, comparingByValue());
   }

   public void reportDaysWithMostCancellations(ReportContext context) {
      byDaysWithCancellations(context, comparingByValue(reverseOrder()));
   }

   private void byDaysWithCancellations(ReportContext context, 
      Comparator<Entry<ChronoLocalDate, Long>> comparator) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparator, limit, 
         new CountingAccumulator<Flight, ChronoLocalDate>() {
            @Override public boolean filter(Flight source) {
               return source.cancelled();
            }
            @Override public ChronoLocalDate getKey(Flight source) {
               return source.getDate();
            }
            @Override public void forEach(Entry<ChronoLocalDate, Long> entry) {
               context.getTerminal()
                      .printf("%-10s       %,3d\n", 
                              entry.getKey(), 
                              entry.getValue());
            }
         }
      );
   }

   public void reportTotalMonthlyFlights(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByKey(), limit, 
         new CountingAccumulator<Flight, YearMonth>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public YearMonth getKey(Flight source) {
               return source.getYearMonth();
            }
            @Override public void forEach(Entry<YearMonth, Long> entry) {
               context.getTerminal()
                      .printf("%s\t%,10d\n", 
                              YEAR_MONTH_FORMAT.format(entry.getKey()), 
                              entry.getValue());
            }
         }
      );
   }

   public void reportTotalDailyFlights(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByKey(), limit, 
         new CountingAccumulator<Flight, ChronoLocalDate>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public ChronoLocalDate getKey(Flight source) {
               return source.getDate();
            }
            @Override public void forEach(Entry<ChronoLocalDate, Long> entry) {
               context.getTerminal()
                      .printf("%s\t%,10d\n", entry.getKey(), entry.getValue());
            }
         }
      );
   }

   public void reportTotalFlightsByDayOfWeek(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByKey(), limit, 
         new CountingAccumulator<Flight, DayOfWeek>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public DayOfWeek getKey(Flight source) {
               return source.getDate().getDayOfWeek();
            }
            @Override public void forEach(Entry<DayOfWeek, Long> entry) {
               context.getTerminal()
                      .printf("%10s\t%,10d\n", 
                              entry.getKey(), 
                              entry.getValue());
            }
         }
      );
   }

   public void reportMostFlightsByDay(ReportContext context) {
      byDay(context, comparingByValue(reverseOrder()));
   }

   public void reportLeastFlightsByDay(ReportContext context) {
      byDay(context, comparingByValue(reverseOrder()));
   }

   private void byDay(ReportContext context, 
      Comparator<Entry<ChronoLocalDate, Long>> comparator) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparator, limit, 
         new CountingAccumulator<Flight, ChronoLocalDate>() {
            @Override public boolean filter(Flight source) {
               return source.notCancelled();
            }
            @Override public ChronoLocalDate getKey(Flight source) {
               return source.getDate();
            }
            @Override public void forEach(Entry<ChronoLocalDate, Long> entry) {
               context.getTerminal()
                      .printf("%s\t%,10d\n", entry.getKey(), entry.getValue());
            }
         }
      );
   }

   public void reportMostFlightsByOriginByDay(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new CountingAccumulator<Flight, PairGroup<Airport, LocalDate>>() {
            @Override public boolean filter(Flight flight) {
               return flight.notCancelled();
            }
            @Override public PairGroup<Airport, LocalDate> getKey(
               Flight flight) {
               return new PairGroup<Airport, LocalDate>(flight.getOrigin(), 
                                                        flight.getDate());
            }
            @Override public void forEach(Entry<PairGroup<Airport, LocalDate>, 
               Long> entry) {
               PairGroup<Airport, LocalDate> key = entry.getKey();
               context.getTerminal()
                      .printf("%-30s\t%s\t%,10d\n", 
                              left(key.getFirst().getName(), 30), 
                              key.getSecond(), 
                              entry.getValue()
               );
            }
         }
      );
   }

   public void reportMostFlightsByCarrierByDay(ReportContext context) {
      final int year = context.getYear();
      final int limit = context.getLimit();

      Iterator<Flight> iterator = context.getRepository().getFlightIterator(year);
      accumulate(iterator, comparingByValue(reverseOrder()), limit, 
         new CountingAccumulator<Flight, PairGroup<Carrier, LocalDate>>() {
            @Override public boolean filter(Flight flight) {
               return flight.notCancelled();
            }
            @Override public PairGroup<Carrier, LocalDate> getKey(
               Flight flight) {
               return new PairGroup<Carrier, LocalDate>(flight.getCarrier(), 
                                                        flight.getDate());
            }
            @Override public void forEach(
               Entry<PairGroup<Carrier, LocalDate>, Long> entry) {
               PairGroup<Carrier, LocalDate> key = entry.getKey();
               context.getTerminal()
                      .printf("%-30s\t%s\t%,10d\n", 
                              left(key.getFirst().getName(), 30), 
                              key.getSecond(), 
                              entry.getValue()
               );
            }
         }
      );
   }
}