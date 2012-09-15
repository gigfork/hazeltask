package com.hazeltask;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazeltask.clustertasks.IsMemberReadyTask;
import com.hazeltask.clustertasks.NoOpTask;
import com.hazeltask.config.HazeltaskConfig;
import com.hazeltask.hazelcast.MemberTasks;
import com.hazeltask.hazelcast.MemberTasks.MemberResponse;
import com.succinctllc.hazelcast.work.HazelcastWork;

public class HazelcastTopologyService implements ITopologyService {
private String topologyName;
    
    private final ExecutorService communicationExecutorService;
    private final HazelcastInstance hazelcast;
    
    //TOOD: pass in the communication service?
    //or make the svc accessible to the ExecutorTopologyService? so we don't have to manage
    //the name in 2 places
    public HazelcastTopologyService(HazeltaskConfig hazeltaskConfig) {
        topologyName = hazeltaskConfig.getTopologyName();
        hazelcast = hazeltaskConfig.getHazelcast();
        communicationExecutorService = hazelcast.getExecutorService(name("com"));
    }
    
    private String name(String name) {
        return topologyName + "-" + name;
    }
    
    
    public long pingMember(Member member) {
        try {
            long start = System.currentTimeMillis();
            DistributedTask<Object> ping = new DistributedTask<Object>(new NoOpTask(), member);
            communicationExecutorService.submit(ping).get(4, TimeUnit.SECONDS);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Member> getReadyMembers() {
        Collection<MemberResponse<Boolean>> responses = MemberTasks.executeOptimistic(
                communicationExecutorService, hazelcast.getCluster().getMembers(),
                new IsMemberReadyTask(topologyName));
        Set<Member> result = new HashSet<Member>(responses.size());
        for(MemberResponse<Boolean> response : responses) {
            if(response.getValue())
                result.add(response.getMember());
        }
        return result;
    }
    
    public void shutdown() {
        // TODO Auto-generated method stub
        
    }
    
    public List<HazelcastWork> shutdownNow() {
        // TODO Auto-generated method stub
        return null;
    }
}