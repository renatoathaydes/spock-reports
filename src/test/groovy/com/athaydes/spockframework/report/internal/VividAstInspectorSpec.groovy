package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.vivid.VividAstInspector
import spock.lang.Specification
import spock.lang.Subject

class VividAstInspectorSpec extends Specification {

    @Subject
    final inspector = new VividAstInspector()

    def "Vivid AST Inspector can load single line code blocks from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    given: 'the given block'
        |    def x = 10
        |
        |    when: 'an action is taken'
        |    x += 10
        |   
        |    then: 'the result is right'
        |    x == 20 
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        then: 'The inspector should be able to provide the source code for each block'
        def given = result.getLines( 'my feature', 0 )
        given == [ 'def x = 10' ]

        def when = result.getLines( 'my feature', 1 )
        when == [ 'x += 10' ]

        def then = result.getLines( 'my feature', 2 )
        then == [ 'x == 20' ]
    }

    def "Vivid AST Inspector can load multi-line code blocks from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    when:
        |    def x = 10
        |    def y = 20
        |    def z = x + y
        |
        |    then: 'the result is right'
        |    x + y == z
        |    assert x * 2 == y
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        then: 'The inspector should be able to provide the source code for each block'
        def when = result.getLines( 'my feature', 0 )
        when == [ 'def x = 10', 'def y = 20', 'def z = x + y' ]

        def then = result.getLines( 'my feature', 1 )
        then == [ 'x + y == z', 'assert x * 2 == y' ]
    }

    def "Vivid AST Inspector can load multi-line expressions from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    when:
        |    def x = 10 +
        |      20 +
        |      30
        |
        |    and:
        |    def y = """
        |      hello
        |      world
        |    """
        |
        |    then: 'the result is right'
        |    x ==
        |    60
        |
        |    and:
        |    y == 'hello world'
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        then: 'The inspector should be able to provide the source code for each block'
        def when = result.getLines( 'my feature', 0 )
        when == [ 'def x = 10 +\n      20 +\n      30' ]

        def when2 = result.getLines( 'my feature', 1 )
        when2 == [ 'def y = """\n' +
                           '      hello\n' +
                           '      world\n' +
                           '    """' ]

        def then = result.getLines( 'my feature', 2 )
        then == [ 'x ==\n    60' ]

        def then2 = result.getLines( 'my feature', 3 )
        then2 == [ "y == 'hello world'" ]
    }

    def "Vivid AST Inspector does not load the where and cleanup code blocks from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class SomeSpec extends Specification {
        |  def "examples feature"() {
        |    expect:
        |    x < y
        |
        |    where: 'the examples are'
        |    x   | y
        |    20  | 30
        |    100 | 2000
        |
        |    cleanup: 'Just pretend to cleanup'
        |    x = null
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'SomeSpec' )

        then: 'The inspector should be able to provide the source code for each block'
        def then = result.getLines( 'examples feature', 0 )
        then == [ 'x < y' ]

        and: 'The where block is not captured'
        def where = result.getLines( 'examples feature', 1 )
        where.isEmpty()

        and: 'The cleanup block is not captured'
        def cleanup = result.getLines( 'examples feature', 2 )
        cleanup.isEmpty()
    }

}
