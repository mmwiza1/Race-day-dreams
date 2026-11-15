"""Emit reproducible shared Kotlin with the repository's existing pinned build-time formatter.

Projection/coordinate calculations remain in their original generators. Formatting requires
Java and the reviewed formatter cache (downloaded with its pinned checksum when cold).
This tooling is never packaged into the application.
"""
from pathlib import Path
import importlib.util
import json
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent.parent
SHARED = ROOT / 'sharedDomain/src/commonMain/kotlin/org/racedaydreams/domain/support'


def format_source(name, source):
    spec = importlib.util.spec_from_file_location('rdd_pinned_formatter', ROOT / 'scripts/format-code.py')
    formatter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(formatter)
    tool = json.loads(formatter.CONFIG.read_text())['tools']['kotlin']
    cache = Path(os.environ.get('RDD_FORMATTER_CACHE', ROOT / '.gradle/rdd-formatters'))
    jar = formatter.tool_jar('kotlin', tool, cache)
    with tempfile.TemporaryDirectory(prefix='rdd-dream-kotlin-') as directory:
        path = Path(directory) / (name + '.kt')
        path.write_text(source)
        for _ in range(formatter.MAX_FORMAT_PASSES):
            before = path.read_bytes()
            result = subprocess.run(formatter.command('kotlin', tool, jar, [path], True), capture_output=True, text=True)
            if result.returncode:
                raise RuntimeError(result.stderr or result.stdout)
            if path.read_bytes() == before:
                return path.read_text()
        raise RuntimeError('Generated Kotlin formatter did not converge')


def geometry_source(name, route_id, label, points, notice):
    chunks = [points[i:i+128] for i in range(0, len(points), 128)]
    source = 'package org.racedaydreams.domain.support\n\nimport kotlin.jvm.JvmStatic\n\n'
    source += notice + '\ninternal object ' + name + ' {\n    @JvmStatic fun route():DreamRoutes.Route {\n'
    source += '        val points=ArrayList<DreamRoutes.Vertex>(' + str(len(points)) + ')\n'
    for index in range(len(chunks)):
        source += '        points.addAll(part' + str(index) + '())\n'
    source += '        return DreamRoutes.Route(' + json.dumps(route_id, ensure_ascii=False) + ',' + json.dumps(label, ensure_ascii=False) + ',points)\n    }\n'
    for index, chunk in enumerate(chunks):
        source += '    private fun part' + str(index) + '():List<DreamRoutes.Vertex> = listOf(\n'
        source += ''.join('        DreamRoutes.Vertex(%.8f,%.8f),\n' % point for point in chunk)
        source += '    )\n'
    return format_source(name, source + '}\n')
