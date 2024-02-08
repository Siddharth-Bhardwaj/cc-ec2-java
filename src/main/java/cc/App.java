package cc;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateTagsResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

import java.util.List;

public class App {
    public static void main(String[] args) throws InterruptedException {
        String sgName = "securityGroupForDemo";
        String sgDesc = "This is a security group for demo";
        String keyName = "COMS-6998-demo-key";
        String instanceName = "sid-demo-instance";
        String amiId = "ami-06b263d6ceff0b3dd"; // Ubuntu 18.04 LTS
        int minInstance = 1;
        int maxInstance = 1;
        createSecurityGroup(sgName, sgDesc);
        createKeyPair(keyName);
        createInstance(instanceName, amiId, sgName, keyName, minInstance, maxInstance);
    }

    public static void createSecurityGroup(String groupName, String desc) {
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
        CreateSecurityGroupRequest createRequest = new CreateSecurityGroupRequest()
                .withGroupName(groupName)
                .withDescription(desc);
        CreateSecurityGroupResult createResponse = ec2.createSecurityGroup(createRequest);
    }

    public static void createKeyPair(String keyName) {
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
        CreateKeyPairRequest request = new CreateKeyPairRequest().withKeyName(keyName);
        CreateKeyPairResult response = ec2.createKeyPair(request);
    }

    public static void createInstance(String name, String amiId, String sgName, String keyName, int min, int max) throws InterruptedException {
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
        RunInstancesRequest runRequest = new RunInstancesRequest()
                .withImageId(amiId)
                .withInstanceType(InstanceType.T2Micro)
                .withMaxCount(min)
                .withMinCount(max)
                .withKeyName(keyName)
                .withSecurityGroups(sgName);
        RunInstancesResult runResponse = ec2.runInstances(runRequest);
        String reservationId = runResponse.getReservation().getInstances().get(0).getInstanceId();
        System.out.printf("EC2 instance %s started based on AMI %s\n", reservationId, amiId);
        Thread.sleep(1000);
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        List<String> instanceIds = new java.util.ArrayList<>();
        instanceIds.add(reservationId);
        describeInstancesRequest.setInstanceIds(instanceIds);
        while (true) {
            DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
            if ("running".equalsIgnoreCase(describeInstancesResult.getReservations().get(0).getInstances().get(0).getState().getName())) {
                System.out.println("Instance started running with public IP: "
                        + describeInstancesResult.getReservations().get(0).getInstances().get(0).getPublicIpAddress()
                + " and private IP: " + describeInstancesResult.getReservations().get(0).getInstances().get(0).getPrivateIpAddress());
                break;
            } else {
                Thread.sleep(3000);
            }
        }

        Tag tag = new Tag()
                .withKey("Name")
                .withValue(name);
        CreateTagsRequest tagRequest = new CreateTagsRequest()
                .withResources(reservationId)
                .withTags(tag);
        CreateTagsResult tagResponse = ec2.createTags(tagRequest);
        tagResponse.getSdkResponseMetadata().getRequestId();
    }
}
