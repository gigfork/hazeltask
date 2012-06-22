package com.succinctllc.core.concurrent.collections.grouped;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.ImmutableMap;
import com.succinctllc.core.concurrent.collections.grouped.GroupedQueueRouter.GroupedRouter;
import com.succinctllc.core.concurrent.collections.tracked.ITrackedQueue;
import com.succinctllc.core.concurrent.collections.tracked.TrackedQueue;

public class GroupedQueue<E extends Groupable> extends AbstractQueue<E> implements IGroupedQueue<E>  {
	private final ConcurrentMap<String, ITrackedQueue<E>> queuesByGroup;
	private final CopyOnWriteArrayList<String> groups = new CopyOnWriteArrayList<String>();
	private final GroupedRouter<E> groupRouter;
	
	public static interface PartitionQueueListener<E> {
		public void onPartitionCreated(String partition, TrackedQueue<E> q);
	}
	
	public GroupedQueue(GroupedRouter<E> partitionRouter){
		queuesByGroup = createQueuesByGroupMap();
		this.groupRouter = partitionRouter == null 
		                            ? new GroupedQueueRouter.InOrderRouter<E>() 
		                            : partitionRouter;
		this.groupRouter.setPartitionedQueueue(this);
	}
	
	public GroupedQueue(){
        this(null);
    }
	
	public Map<String, ITrackedQueue<E>> getQueuesByGroup() {
		return Collections.unmodifiableMap(this.queuesByGroup);
	}
	
	public ITrackedQueue<E> getQueueByGroup(String group){
	    return this.queuesByGroup.get(group);
	}
	
	public List<String> getGroups() {
		return groups;
	}
	
	private ConcurrentMap<String, ITrackedQueue<E>> createQueuesByGroupMap() {
		return new ConcurrentHashMap<String, ITrackedQueue<E>>();
	}
	
	protected TrackedQueue<E> createQueue(){
		return new TrackedQueue<E>();
	}
	
	private Queue<E> getOrCreateGroupQueue(String group) {
		Queue<E> q = queuesByGroup.get(group);
		if(q == null) {
			ITrackedQueue<E> newQ = createQueue();
			if(queuesByGroup.putIfAbsent(group, newQ) == null) {
				q = newQ;
				groups.add(group);
			} else {
				q = queuesByGroup.get(group);
			}
		}
		return q;
	}

	public boolean offer(E e) {
		String partition = e.getGroup();
		Queue<E> q = getOrCreateGroupQueue(partition);
		boolean result = q.offer(e);
		return result;
	}
	
	
	//FIXME: guarantee that it will return non-null if queue is not empty (up to router?)
	public E peek() {
		ITrackedQueue<E> oldestQueue = groupRouter.peekPartition();
		if(oldestQueue != null)
			return oldestQueue.peek();
		else
			return null;
	}

	//FIXME: guarantee that it will return non-null if queue is not empty (up to router?)
	public E poll() {
		ITrackedQueue<E> oldestQueue = groupRouter.nextPartition();
		if(oldestQueue != null)
			return oldestQueue.poll();
		else
			return null;
	}
	

	/**
	 * This iterator does not guarantee it will iterate in insertion order.  It will
	 * simply iterate each partition queue one by one
	 * @return
	 */
	@Override
	public Iterator<E> iterator() {
		return new FastPartitionedQueueIterator();
	}
	
	public class FastPartitionedQueueIterator implements Iterator<E> {
		private final List<Iterator<E>> queueIterators; 
		private int currentQueue = 0;
		
		public FastPartitionedQueueIterator(){
			queueIterators = new ArrayList<Iterator<E>>(queuesByGroup.size());
			for(ITrackedQueue<E> q : queuesByGroup.values()) {
				queueIterators.add(q.iterator());
			}
		}
		
		private Iterator<E> getIterator(){
			if(currentQueue >= queueIterators.size()) {
				return null;
			}
			
			Iterator<E> it = queueIterators.get(currentQueue);	
			
			do {
				if(it.hasNext()) {
					return it;
				}
				currentQueue++;
				if(currentQueue >= queueIterators.size()) {
				    return null;
				}
				
				it = queueIterators.get(currentQueue);
			} while(currentQueue < queueIterators.size());
			
			if(it.hasNext()) {
				return it;
			} else {
				return null;
			}
		}
		
		public boolean hasNext() {
			Iterator<E> it = getIterator();
			if(it != null)
				return true;
			else
				return false;
		}

		public E next() {
			Iterator<E> it = getIterator();
			if(it != null)
				return it.next();
			else
				return null;
		}

		public void remove() {
			Iterator<E> it = queueIterators.get(currentQueue);
			if(it != null)
				it.remove();
		}
		
	}
	
	public Long getOldestQueueTime() {
	    long oldestTime = Long.MAX_VALUE;
	    for(Entry<String, ITrackedQueue<E>> entry : this.queuesByGroup.entrySet()) {
	        Long oldest = entry.getValue().getOldestItemTime();
	        if(oldest != null && oldest < oldestTime)
                oldestTime = oldest;
        }
        
        if(oldestTime == Long.MAX_VALUE)
            return null;
	    
	    return oldestTime;
	}

	@Override
	public int size() {
		int size = 0;
		for(Queue<E> q : queuesByGroup.values()) {
			size += q.size();
		}
		return size;
	}

	public int drainTo(String partition, Collection<? super E> toCollection) {
		E elem = null;
		int num = 0;
		Queue<E> q = getQueuesByGroup().get(partition);
		while((elem = q.poll()) != null) {
			toCollection.add(elem);
			num++;
		}
		return num;
	}

	public int drainTo(String partition, Collection<? super E> toCollection, int max) {
		E elem = null;
		int num = 0;
		Queue<E> q = getQueuesByGroup().get(partition);
		while(num < max && (elem = q.poll()) != null) {
			toCollection.add(elem);
			num++;
		}
		return num;
	}	
}
