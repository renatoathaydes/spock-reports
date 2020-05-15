package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.vivid.VividAstInspector
import spock.lang.Specification
import spock.lang.Subject

class VividAstInspectorSpec extends Specification {

    @Subject
    def inspector = new VividAstInspector()

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

        and: 'The code blocks are requested'
        def blocks = result.getBlocks( 'my feature' )

        then: 'The correct number of blocks is parsed'
        blocks.size() == 3

        and: 'The inspector should be able to provide the source code for each block'
        blocks[ 0 ].statements == [ 'def x = 10' ]
        blocks[ 1 ].statements == [ 'x += 10' ]
        blocks[ 2 ].statements == [ 'x == 20' ]

        and: 'The blocks should have the expected label and text'
        blocks[ 0 ].label == 'given'
        blocks[ 0 ].text == 'the given block'
        blocks[ 1 ].label == 'when'
        blocks[ 1 ].text == 'an action is taken'
        blocks[ 2 ].label == 'then'
        blocks[ 2 ].text == 'the result is right'
    }

    def "Vivid AST Inspector can load empty code blocks from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "A first test with Then code block"() {
        |    given:
        |    "we have x and y"
        |
        |    and:
        |    "some more things"
        |
        |    when:
        |    "I do crazy things"
        |
        |    and:
        |
        |    then:
        |      x == y
        |
        |    where:
        |    "The examples below are used"
        |      x | y
        |    'a' | 'a'
        |    'b' | 'c'        
        |  }
        |}
        |'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        and: 'The code blocks are requested'
        def blocks = result.getBlocks( 'A first test with Then code block' )

        then: 'The correct number of blocks is parsed'
        blocks.size() == 5

        and: 'The inspector should be able to provide the source code for each block'
        blocks[ 0 ].statements.isEmpty()
        blocks[ 1 ].statements.isEmpty()
        blocks[ 2 ].statements.isEmpty()
        blocks[ 3 ].statements == [ 'x == y' ]
        blocks[ 4 ].statements.isEmpty() // where statements are not captured

        and: 'The blocks should have the expected label and text'
        blocks[ 0 ].label == 'given'
        blocks[ 0 ].text == 'we have x and y'
        blocks[ 1 ].label == 'and'
        blocks[ 1 ].text == 'some more things'
        blocks[ 2 ].label == 'when'
        blocks[ 2 ].text == 'I do crazy things'
        blocks[ 3 ].label == 'then'
        blocks[ 3 ].text == null
        blocks[ 4 ].label == 'where'
        blocks[ 4 ].text == 'The examples below are used'
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

        and: 'The code blocks are requested'
        def blocks = result.getBlocks( 'my feature' )

        then: 'The correct number of blocks is parsed'
        blocks.size() == 2

        and: 'The inspector should be able to provide the source code for each block'
        blocks[ 0 ].statements == [ 'def x = 10', 'def y = 20', 'def z = x + y' ]
        blocks[ 1 ].statements == [ 'x + y == z', 'assert x * 2 == y' ]

        and: 'The blocks should have the expected label and text'
        blocks[ 0 ].label == 'when'
        blocks[ 0 ].text == null
        blocks[ 1 ].label == 'then'
        blocks[ 1 ].text == 'the result is right'
    }

    def "Vivid AST Inspector can parse specifications containing methods that look like test methods but are not"() {
        given: 'A Groovy source file containing a method within a Specification that is public and takes no args'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    when:
        |    def x = notTest()
        |
        |    then: 'the result is right'
        |    x == 20
        |  }
        |
        |  def notTest() {
        |    int i = 10
        |    return i * 2    
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        and: 'The code blocks are requested'
        def blocks = result.getBlocks( 'my feature' )

        then: 'The correct number of blocks is parsed'
        blocks.size() == 2

        and: 'The inspector should be able to provide the source code for each block'
        blocks[ 0 ].statements == [ 'def x = notTest()' ]
        blocks[ 1 ].statements == [ 'x == 20' ]

        and: 'The blocks should have the expected label and text'
        blocks[ 0 ].label == 'when'
        blocks[ 0 ].text == null
        blocks[ 1 ].label == 'then'
        blocks[ 1 ].text == 'the result is right'
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

        and: 'The code blocks are requested'
        def blocks = result.getBlocks( 'my feature' )

        then: 'The correct number of blocks is parsed'
        blocks.size() == 4

        and: 'The inspector should be able to provide the source code for each block'
        blocks[ 0 ].statements == [ 'def x = 10 +', '20 +', '30' ]
        blocks[ 1 ].statements == [ 'def y = """',
                                    '  hello',
                                    '  world',
                                    '"""' ]
        blocks[ 2 ].statements == [ 'x ==', '60' ]
        blocks[ 3 ].statements == [ "y == 'hello world'" ]

        and: 'The blocks should have the expected label and text'
        blocks[ 0 ].label == 'when'
        blocks[ 0 ].text == null
        blocks[ 1 ].label == 'and'
        blocks[ 1 ].text == null
        blocks[ 2 ].label == 'then'
        blocks[ 2 ].text == 'the result is right'
        blocks[ 3 ].label == 'and'
        blocks[ 3 ].text == null

    }

    def "Vivid AST Inspector does not load statements of the where block from Groovy Specification files"() {
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

        and: 'The code blocks are requested'
        def blocks = result.getBlocks( 'examples feature' )

        then: 'The correct number of blocks is parsed'
        blocks.size() == 3

        and: 'The inspector should be able to provide the source code for each block'
        blocks[ 0 ].statements == [ 'x < y' ]

        and: 'The where block is not captured'
        blocks[ 1 ].statements.isEmpty()

        and: 'The cleanup block is captured'
        blocks[ 2 ].statements == [ 'x = null' ]

        and: 'The blocks should have the expected label and text'
        blocks[ 0 ].label == 'expect'
        blocks[ 0 ].text == null
        blocks[ 1 ].label == 'where'
        blocks[ 1 ].text == 'the examples are'
        blocks[ 2 ].label == 'cleanup'
        blocks[ 2 ].text == 'Just pretend to cleanup'
    }

    def "Vivid AST Inspector captures all nested statements from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    given: 'the given block'
        |    def x = []
        |    for (i in 0..10) {
        |      x << i
        |      x << i * 2
        |    }
        |    int i = 0
        |    while (i < 4) {
        |      for (j in 0..i) {
        |        x << [i, j]
        |      }
        |      println x
        |    }
        |
        |    when: 'an action is taken'
        |    def y = x.collect { item ->
        |      (0..item).filter { i ->
        |        if (i > 4) {
        |          true
        |        } else false
        |      }
        |    }
        |
        |    then: 'the result is right'
        |    x.collect { i ->
        |      i as String
        |    } == [ '1', '2' ]
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        and: 'The code blocks are requested'
        def blocks = result.getBlocks( 'my feature' )

        then: 'The correct number of blocks is parsed'
        blocks.size() == 3

        and: 'The inspector should be able to provide the source code for each block'
        blocks[ 0 ].statements == [ 'def x = []',
                                    'for (i in 0..10) {',
                                    '  x << i',
                                    '  x << i * 2',
                                    '}',
                                    'int i = 0',
                                    'while (i < 4) {',
                                    '  for (j in 0..i) {',
                                    '    x << [i, j]',
                                    '  }',
                                    '  println x',
                                    '}' ]

        blocks[ 1 ].statements == [ 'def y = x.collect { item ->',
                                    '  (0..item).filter { i ->',
                                    '    if (i > 4) {',
                                    '      true',
                                    '    } else false',
                                    '  }',
                                    '}' ]

        blocks[ 2 ].statements == [ 'x.collect { i ->',
                                    '  i as String',
                                    "} == [ '1', '2' ]" ]

        and: 'The blocks should have the expected label and text'
        blocks[ 0 ].label == 'given'
        blocks[ 0 ].text == 'the given block'
        blocks[ 1 ].label == 'when'
        blocks[ 1 ].text == 'an action is taken'
        blocks[ 2 ].label == 'then'
        blocks[ 2 ].text == 'the result is right'
    }

}
