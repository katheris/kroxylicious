# CHANGELOG

Please enumerate **all user-facing** changes using format `<githib issue/pr number>: <short description>`, with changes ordered in reverse chronological order.

## 0.5.0

* [#1004](https://github.com/kroxylicious/kroxylicious/pull/1004): Publish images to Quay kroxylicious/kroxylicious rather than kroxylicious-developer
* [#997](https://github.com/kroxylicious/kroxylicious/issues/997): Add hardcoded maximum frame size
* [#782](https://github.com/kroxylicious/kroxylicious/issues/782): Securely handle the HashiCorp Vault Token in Kroxylicious configuration
* [#973](https://github.com/kroxylicious/kroxylicious/pull/973): Remove deprecated CompositeFilter and its documentation
* [#935](https://github.com/kroxylicious/kroxylicious/pull/935): Enable user to configure alternative source of keys for vault KMS client
* [#787](https://github.com/kroxylicious/kroxylicious/issues/787): Initial documentation for the envelope-encryption feature.
* [#940](https://github.com/kroxylicious/kroxylicious/issues/940): Support vault namespaces and support secrets transit engine at locations other than /transit
* [#951](https://github.com/kroxylicious/kroxylicious/pull/951): Include the kroxylicious maintained filters in the dist by default
* [#910](https://github.com/kroxylicious/kroxylicious/pull/910): Envelope encryption preserve batches within MemoryRecords
* [#883](https://github.com/kroxylicious/kroxylicious/pull/883): Ensure we only initialise a filter factory once.
* [#912](https://github.com/kroxylicious/kroxylicious/pull/912): Bump io.netty:netty-bom from 4.1.104.Final to 4.1.106.Final
* [#909](https://github.com/kroxylicious/kroxylicious/pull/909): [build] use maven maven-dependency-plugin to detect missing/superfluous dependencies at build time
* [#895](https://github.com/kroxylicious/kroxylicious/pull/895): Ensure we execute deferred Filter methods on the eventloop
* [#896](https://github.com/kroxylicious/kroxylicious/issues/896): In TLS config, use passwordFile as property to accept password material from a file rather than filePath.
* [#844](https://github.com/kroxylicious/kroxylicious/pull/844): Fix connect to upstream using TLS client authentication
* [#885](https://github.com/kroxylicious/kroxylicious/pull/885): Bump kroxy.extension.version from 0.8.0 to 0.8.1

### Changes, deprecations and removals

* When configuring TLS, the property `filePath` for specifying the location of a file providing the password is now
  deprecated.  Use `passwordFile` instead.
* When configuring TLS, it is no longer valid to pass a null inline password like `"storePassword": {"password": null}` instead use `"storePassword": null`
* As a result of the work of #909, some superfluous transitive dependencies have been removed from some kroxylicious.  If you were relying on those, you will need to
  adjust your dependencies as your adopt this release.
* `io.kroxylicious:kroxylicious-filter-test-support` now contains RecordTestUtils for creating example `Record`, `RecordBatch` and `MemoryRecords`. It also contains 
  assertj assertions for those same classes to enable us to write fluent assertions, accessible via `io.kroxylicious.test.assertj.KafkaAssertions`.
* The configuration for VaultKMS service has changed.
  * Instead of the `vaultUrl` config key, the provider now requires `vaultTransitEngineUrl`.  This must provide the
    complete path to the Transit Engine on the HashiCorp Vault instance (e.g. https://myvault:8200/v1/transit or
    https://myvault:8200/v1/mynamespace/transit).
  * The `vaultToken` field now requires a `PasswordProvider` object rather than inline text value.   You may pass the
    token from a file (filename specified by a `passwordFile` field) or inline (`password` field).  The latter is not
    recommended in production environments.
* The deprecated CompositeFilter interface has been removed.
* Container images for releases will be published to quay.io/kroxylicious/kroxylicious (rather than kroxylicious-developer)

## 0.4.1

* [#836](https://github.com/kroxylicious/kroxylicious/pull/836): Cache decrypted EDEK and resolved aliases
* [#823](https://github.com/kroxylicious/kroxylicious/pull/823): Recover from EDEK decryption failures and improve KMS resilience measures
* [#841](https://github.com/kroxylicious/kroxylicious/issues/841): Ensure the envelope encryption filter transits record offsets unchanged.
* [#847](https://github.com/kroxylicious/kroxylicious/pull/847): Bump org.apache.maven.plugins:maven-compiler-plugin from 3.11.0 to 3.12.1
* [#838](https://github.com/kroxylicious/kroxylicious/issues/838): Ensure the decryption maintains record ordering, regardless of completion order of the decryptor.
* [#837](https://github.com/kroxylicious/kroxylicious/pull/837): refactor: take advantage of the topic injection in several integration tests including (the SampleFilterIT)
* [#827](https://github.com/kroxylicious/kroxylicious/issues/827): Release process should update version number references in container image versions too
* [#825](https://github.com/kroxylicious/kroxylicious/pull/825): Improve the topic encryption example
* [#832](https://github.com/kroxylicious/kroxylicious/pull/832): Bump io.netty:netty-bom from 4.1.101.Final to 4.1.104.Final
* [#828](https://github.com/kroxylicious/kroxylicious/pull/828): Bump io.micrometer:micrometer-bom from 1.12.0 to 1.12.1

## 0.4.0

* [#817](https://github.com/kroxylicious/kroxylicious/pull/817): Encryption Filter: Set hardcoded request timeout on Vault requests
* [#798](https://github.com/kroxylicious/kroxylicious/pull/798): Encryption Filter: Refactor Serialization to new Parcel Scheme
* [#809](https://github.com/kroxylicious/kroxylicious/pull/809): Bump Kroxylicious Junit Ext from 0.7.0 to 0.8.0
* [#803](https://github.com/kroxylicious/kroxylicious/pull/803): Bump kafka.version from 3.6.0 to 3.6.1 #803
* [#741](https://github.com/kroxylicious/kroxylicious/issues/741): Encryption Filter: Implement a HashiCorp Vault KMS 
* [#764](https://github.com/kroxylicious/kroxylicious/pull/764): Encryption Filter: Rotate to a new DEK when the old one is exhausted
* [#696](https://github.com/kroxylicious/kroxylicious/pull/696): Initial work on an Envelope Encryption Filter
* [#752](https://github.com/kroxylicious/kroxylicious/issues/752): Remove redundant re-installation of time-zone data in Dockerfile used for Kroxylicious container image
* [#727](https://github.com/kroxylicious/kroxylicious/pull/727): Tease out simple transform filters into their own module
* [#628](https://github.com/kroxylicious/kroxylicious/pull/628): Kroxylicious system tests
* [#738](https://github.com/kroxylicious/kroxylicious/pull/738): Update to Kroxylicious Junit Ext 0.7.0
* [#723](https://github.com/kroxylicious/kroxylicious/pull/723): Bump com.fasterxml.jackson:jackson-bom from 2.15.3 to 2.16.0 #723
* [#724](https://github.com/kroxylicious/kroxylicious/pull/724): Bump io.netty.incubator:netty-incubator-transport-native-io_uring from 0.0.23.Final to 0.0.24.Final
* [#725](https://github.com/kroxylicious/kroxylicious/pull/725): Bump io.netty:netty-bom from 4.1.100.Final to 4.1.101.Final #725
* [#710](https://github.com/kroxylicious/kroxylicious/pull/710): Rename modules
* [#709](https://github.com/kroxylicious/kroxylicious/pull/709): Add a KMS service API and an in-memory implementation
* [#667](https://github.com/kroxylicious/kroxylicious/pull/667): Nested factories
* [#701](https://github.com/kroxylicious/kroxylicious/pull/701): Bump org.apache.logging.log4j:log4j-bom from 2.21.0 to 2.21.1 #701

### Changes, deprecations and removals

* The `ProduceRequestTransformationFilter` and `FetchResponseTransformationFilter` have been moved to their own module kroxylicious-simple-transform.
  If you were depending on these filters, you must ensure that the kroxylicious-simple-transform JAR file is added to your classpath.  The
  Javadoc of these classes has been updated to convey the fact that these filters are *not* intended for production use.

## 0.3.0

* [#686](https://github.com/kroxylicious/kroxylicious/pull/686): Bump org.apache.logging.log4j:log4j-bom from 2.20.0 to 2.21.0.
* [#634](https://github.com/kroxylicious/kroxylicious/issues/634): Update integration tests JDK dependency to 21.
* [#632](https://github.com/kroxylicious/kroxylicious/pull/632): Kroxylicious tester now supports creating & deleting topics on specific virtual clusters.
* [#675](https://github.com/kroxylicious/kroxylicious/pull/675): Bump to Netty 4.1.100.Final to mitigate the Rapid Reset Attack (CVE-2023-44487)
* [#665](https://github.com/kroxylicious/kroxylicious/pull/665): Bump org.apache.kafka:kafka-clients from 3.5.1 to 3.6.0
* [#660](https://github.com/kroxylicious/kroxylicious/pull/660): Use container registry neutral terminology in docs/scripts #660
* [#648](https://github.com/kroxylicious/kroxylicious/pull/648): Bump io.netty.incubator:netty-incubator-transport-native-io_uring from 0.0.22.Final to 0.0.23.Final
* [#649](https://github.com/kroxylicious/kroxylicious/pull/649): Bump io.netty:netty-bom from 4.1.97.Final to 4.1.99.Final
* [#650](https://github.com/kroxylicious/kroxylicious/pull/650): Bump io.sundr:builder-annotations from 0.100.3 to 0.101.0
* [#518](https://github.com/kroxylicious/kroxylicious/issues/518): [Breaking] #sendRequest ought to accept request header.
* [#623](https://github.com/kroxylicious/kroxylicious/pull/623): [Breaking] Refactor how Filters are Created
* [#633](https://github.com/kroxylicious/kroxylicious/pull/633): Address missing exception handling in FetchResponseTransformationFilter (and add unit tests)
* [#537](https://github.com/kroxylicious/kroxylicious/issues/537): Computation stages chained to the CompletionStage return by #sendRequest using the default executor async methods now run on the Netty Event Loop.
* [#612](https://github.com/kroxylicious/kroxylicious/pull/612): [Breaking] Allow filter authors to declare when their filter requires configuration. Note this includes a backwards incompatible change to the contract of the `Contributor`. `getInstance` will now throw exceptions rather than returning `null` to mean there was a problem or this contributor does not know about the requested type.
* [#608](https://github.com/kroxylicious/kroxylicious/pull/608): Improve the contributor API to allow it to express more properties about the configuration. This release deprecates `Contributor.getConfigType` in favour of `Contributor.getConfigDefinition`. It also removes the proliferation of ContributorManager classes by providing a single type which can handle all Contributors.
* [#538](https://github.com/kroxylicious/kroxylicious/pull/538): Refactor FilterHandler and fix several bugs that would leave messages unflushed to client/broker.
* [#531](https://github.com/kroxylicious/kroxylicious/pull/531): Simple Test Client now supports multi-RPC conversations with the server.
* [#510](https://github.com/kroxylicious/kroxylicious/pull/510): Add multi-tenant kubernetes example
* [#519](https://github.com/kroxylicious/kroxylicious/pull/519): Fix Kafka Client leaks in the SampleFilterIntegrationTest.
* [#494](https://github.com/kroxylicious/kroxylicious/issues/494): [Breaking] Make the Filter API fully asynchronous (filter methods must return a CompletionStage)
* [#498](https://github.com/kroxylicious/kroxylicious/issues/498): Include the cluster name from the configuration node in the config model.
* [#488](https://github.com/kroxylicious/kroxylicious/pull/488): Kroxylicious Bill Of Materials 
* [#480](https://github.com/kroxylicious/kroxylicious/issues/480): Multi-tenant - add suport for the versions of OffsetFetch, FindCoordinator, and DeleteTopics used by Sarama client v1.38.1
* [#472](https://github.com/kroxylicious/kroxylicious/issues/472): Respect logFrame/logNetwork options in virtualcluster config
* [#470](https://github.com/kroxylicious/kroxylicious/issues/470): Ensure that the EagerMetadataLearner passes on a client's metadata request with fidelity (fix for kcat -C -E)
* [#416](https://github.com/kroxylicious/kroxylicious/issues/416): Eagerly expose broker endpoints on startup to allow existing client to reconnect (without connecting to bootstrap).
* [#463](https://github.com/kroxylicious/kroxylicious/issues/463): deregister micrometer hooks, meters and the registry on shutdown
* [#443](https://github.com/kroxylicious/kroxylicious/pull/443): Obtain upstream ApiVersions when proxy is not SASL offloading
* [#412](https://github.com/kroxylicious/kroxylicious/issues/412): Remove $(portNumber) pattern from brokerAddressPattern for SniRouting and PortPerBroker schemes
* [#414](https://github.com/kroxylicious/kroxylicious/pull/414): Add kubernetes sample illustrating SNI based routing, downstream/upstream TLS and the use of certificates from cert-manager.
* [#392](https://github.com/kroxylicious/kroxylicious/pull/392): Introduce CompositeFilters
* [#401](https://github.com/kroxylicious/kroxylicious/pull/401): Fix netty buffer leak when doing a short-circuit response
* [#409](https://github.com/kroxylicious/kroxylicious/pull/409): Bump netty.version from 4.1.93.Final to 4.1.94.Final #409 
* [#374](https://github.com/kroxylicious/kroxylicious/issues/374) Upstream TLS support
* [#375](https://github.com/kroxylicious/kroxylicious/issues/375) Support key material in PEM format (X.509 certificates and PKCS-8 private keys) 
* [#398](https://github.com/kroxylicious/kroxylicious/pull/398): Validate admin port does not collide with cluster ports
* [#384](https://github.com/kroxylicious/kroxylicious/pull/384): Bump guava from 32.0.0-jre to 32.0.1-jre
* [#372](https://github.com/kroxylicious/kroxylicious/issues/372): Eliminate the test config model from the code-base
* [#364](https://github.com/kroxylicious/kroxylicious/pull/364): Add Dockerfile for kroxylicious

### Changes, deprecations and removals

The Filter API is refactored to be fully asynchronous.  Filter API methods such as `#onXxxxRequest` and `onXxxxResponse`
now are required to return a `CompletionStage<FilterResult>`. The `FilterResult` encapsulates the message to be
forwarded and carries orders (such as close the connection). The context provides factory methods for creating
`FilterResult` objects.

The default metrics port has changed from 9193 to 9190 to prevent port collisions

Filter Authors can now implement CompositeFilter if they want a single configuration block to contribute multiple Filters
to the Filter chain. This enables them to write smaller, more focused Filter implementations but deliver them as a whole
behaviour with a single block of configuration in the Kroxylicious configuration yaml. This interface is mutually exclusive
with RequestFilter, ResponseFilter or any specific message Filter interfaces.

In the kroxylicious config, the brokerAddressPattern parameter for the PortPerBroker scheme no longer accepts or requires
:$(portNumber) suffix.  In addition, for the SniRouting scheme the config now enforces that there is no port specifier
present on the brokerAddressPattern parameter. Previously, it was accepted but would lead to a failure later.

Kroxylicious configuration no longer requires a non empty `filters` list, users can leave it unset or configure in an empty
list of filters and Kroxylicious will proxy to the cluster successfully.

The Contributor API for creating filters has been significantly changed. 

- `FilterContributor` is renamed `FilterFactory`. 
- Filter Authors will now implement one FilterFactory implementation for each Filter implementation. So the cardinality is now one-to-one.
- We now identify which filter we want to load using it's class name or simple class name,
for example `io.kroxylicious.filter.SpecialFilter` or `SpecialFilter`.
- `FilterConstructContext` is renamed `FilterCreateContext`
- FilterExecutors is removed from FilterCreateContext and the `eventloop()` method is pulled up to FilterCreateContext.
- BaseConfig is removed and any Jackson deserializable type can be used as config.
- configuration is no longer part of the FilterCreateContext, it is supplied as a parameter to the `FilterFactory#createFilter(..)` method.

The names used to identify port-per-broker and sni-routing schemes in the Kroxylicious configuration have changed:
- `PortPerBroker` -> `PortPerBrokerClusterNetworkAddressConfigProvider`
- `SniRouting` -> `SniRoutingClusterNetworkAddressConfigProvider`

The names used to identify micrometer configuration hooks in configuration have changed:
- `CommonTagsContributor` -> `CommonTagsHook`
- `StandardBindersContributor` -> `StandardBindersHook`
- 
#### CVE Fixes
CVE-2023-44487 [#675](https://github.com/kroxylicious/kroxylicious/pull/675)
