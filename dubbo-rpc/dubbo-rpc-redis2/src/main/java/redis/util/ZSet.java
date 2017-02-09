package redis.util;

import java.util.*;

/**
 * Sorted by score, look up by key
 * <p/>
 * User: sam
 * Date: 7/29/12
 * Time: 4:40 PM
 */
public class ZSet implements Iterable<ZSetEntry> {

  private static final BytesKey EMPTY = new BytesKey(new byte[0]);
  // A way to find an entry by key
  private BytesKeyObjectMap<ZSetEntry> map = new BytesKeyObjectMap<ZSetEntry>();
  // A list that we keep sorted by score
  private List<ZSetEntry> list = new ArrayList<ZSetEntry>();

  public ZSet(ZSet destination) {
    map.putAll(destination.map);
    list.addAll(destination.list);
  }

  public ZSet() {
  }

  public int size() {
    return list.size();
  }

  public ZSetEntry get(byte[] member2) {
    return map.get(member2);
  }

  public boolean remove(byte[] member2) {
    return remove(new BytesKey(member2));
  }

  @Override
  public Iterator<ZSetEntry> iterator() {
    return list.iterator();
  }

  public ZSetEntry get(BytesKey key) {
    return map.get(key);
  }

  public List<ZSetEntry> list() {
    return list;
  }

  private static class ScoreComparator implements Comparator<ZSetEntry> {
    @Override
    public int compare(ZSetEntry o1, ZSetEntry o2) {
      double value = o1.getScore() - o2.getScore();
      return value < 0 ? -1 : value == (o1.getKey().compareTo(o2.getKey())) ? 0 : 1;
    }
  }

  public void addAll(ZSet other) {
    for (ZSetEntry zSetEntry : other.list) {
      remove(zSetEntry.getKey());
      add(zSetEntry.getKey(), zSetEntry.getScore());
    }
  }

  public boolean remove(BytesKey key) {
    ZSetEntry current = map.get(key);
    if (current != null) {
      map.remove(key);
      int index = Collections.binarySearch(list, current);
      list.remove(index);
    }
    return current == null;
  }

  public boolean add(BytesKey key, double score) {
    ZSetEntry current = map.get(key);
    if (current != null) {
      map.remove(key);
      int index = Collections.binarySearch(list, current);
      list.remove(index);
    }
    ZSetEntry entry = new ZSetEntry(key, score);
    map.put(key, entry);
    int index = find(Collections.binarySearch(list, entry));
    list.add(index, entry);
    return current == null;
  }

  public Iterable<ZSetEntry> subSet(final int minIndex, int maxIndex) {
    final int finalMaxIndex = Math.min(maxIndex, list.size() - 1);
    return new Iterable<ZSetEntry>() {
      @Override
      public Iterator<ZSetEntry> iterator() {
        return new Iterator<ZSetEntry>() {
          int min = minIndex;

          @Override
          public boolean hasNext() {
            return min <= finalMaxIndex;
          }

          @Override
          public ZSetEntry next() {
            return list.get(min++);
          }

          @Override
          public void remove() {
          }
        };
      }
    };
  }

  public boolean isEmpty() {
    return list.size() == 0;
  }

  public List<ZSetEntry> subSet(double min, double max) {
    int minIndex = find(Collections.binarySearch(list, new ZSetEntry(EMPTY, min)));
    int maxIndex = find(Collections.binarySearch(list, new ZSetEntry(EMPTY, max)));
    if (list.get(maxIndex).getScore() > max) {
      maxIndex = maxIndex - 1;
    }
    return list.subList(minIndex, maxIndex + 1);
  }

  private int find(int minIndex) {
    return minIndex < 0 ? -(minIndex + 1) : minIndex;
  }
}
