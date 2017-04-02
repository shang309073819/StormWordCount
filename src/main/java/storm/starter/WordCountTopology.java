package storm.starter;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.IBasicBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import storm.starter.spout.RandomSentenceSpout;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WordCountTopology {

	public static class SplitSentence implements IBasicBolt {

		private static final long serialVersionUID = 1L;

		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("word"));
		}

		public Map<String, Object> getComponentConfiguration() {
			return null;
		}

		public void prepare(Map stormConf, TopologyContext context) {

		}

		public void execute(Tuple input, BasicOutputCollector collector) {
			String sentence = input.getString(0);
			for (String word : sentence.split(" ")) {
				collector.emit(new Values(word));
			}
		}

		public void cleanup() {
		}

	}

	public static class WordCount extends BaseBasicBolt {
		private final Log log = LogFactory.getLog(getClass().getName());

		// 使用map来统计次数
		Map<String, Integer> counts = new HashMap<String, Integer>();

		public void execute(Tuple tuple, BasicOutputCollector collector) {
			String word = tuple.getString(0);
			Integer count = counts.get(word);
			if (count == null)
				count = 0;
			count++;
			counts.put(word, count);
			collector.emit(new Values(word, count));
			log.info("word:count  " + word + ":" + count);
		}

		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("word", "count"));
		}
	}

	public static void main(String[] args) throws Exception {

		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("spout", new RandomSentenceSpout(), 5);
		builder.setBolt("split", new SplitSentence(), 8).shuffleGrouping(
				"spout");
		builder.setBolt("count", new WordCount(), 12).fieldsGrouping("split",
				new Fields("word"));

		Config conf = new Config();
		conf.setDebug(true);

		if (args != null && args.length > 0) {
			conf.setNumWorkers(3);

			// 集群模式
			// 它接受三个参数：要运行的topology的名字，一个配置对象以及要运行的topology本身。
			StormSubmitter.submitTopology(args[0], conf,
					builder.createTopology());
		} else {
			conf.setMaxTaskParallelism(3);
			LocalCluster cluster = new LocalCluster();

			cluster.submitTopology("word-count", conf, builder.createTopology());
			Thread.sleep(10000);
			cluster.shutdown();
		}
	}
}